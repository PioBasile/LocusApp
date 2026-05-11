package handlers

import (
	"context"
	"fmt"
	"log"
	"strings"
	firebase "firebase.google.com/go/v4"
	"firebase.google.com/go/v4/messaging"
	"google.golang.org/api/option"
)

func newFirebaseClient(ctx context.Context) (*messaging.Client, error) {
	opt := option.WithServiceAccountFile("locusKey.json")
	app, err := firebase.NewApp(ctx, nil, opt)
	if err != nil {
		return nil, err
	}
	return app.Messaging(ctx)
}

// parseCaption strips ---loc: / ---tags: metadata from a post description.
func parseCaption(description string) string {
	caption := description
	if idx := strings.Index(caption, "\n---"); idx >= 0 {
		caption = caption[:idx]
	}
	caption = strings.TrimSpace(caption)
	if caption == "" {
		return "New post"
	}
	if len(caption) > 100 {
		return caption[:97] + "..."
	}
	return caption
}

func NotifyGroupMembersPush(groupIDs []int, authorID int, postDescription string) {
	ctx := context.Background()
	client, err := newFirebaseClient(ctx)
	if err != nil {
		log.Printf("Firebase init error: %v", err)
		return
	}

	var authorUsername string
	_ = db.QueryRow(`SELECT username FROM Utilisateurs WHERE usr_id = $1`, authorID).Scan(&authorUsername)
	if authorUsername == "" {
		authorUsername = "Someone"
	}

	var tokens []string
	query := `
        SELECT DISTINCT u.fcm_token
        FROM Utilisateurs u
        JOIN MembreGroupes mg ON u.usr_id = mg.usr_id
        WHERE mg.id_grp = ANY($1)
          AND u.usr_id != $2
          AND u.fcm_token IS NOT NULL AND u.fcm_token != ''`
	err = db.Select(&tokens, query, groupIDs, authorID)
	if err != nil || len(tokens) == 0 {
		return
	}

	caption := parseCaption(postDescription)
	message := &messaging.MulticastMessage{
		Tokens: tokens,
		Notification: &messaging.Notification{
			Title: fmt.Sprintf("%s posted in your group", authorUsername),
			Body:  caption,
		},
		Data: map[string]string{
			"type": "new_post",
		},
	}

	br, err := client.SendEachForMulticast(ctx, message)
	if err != nil {
		log.Printf("Firebase multicast error: %v", err)
	} else {
		log.Printf("%d notifications sent, %d failures", br.SuccessCount, br.FailureCount)
		for i, resp := range br.Responses {
			if !resp.Success {
				log.Printf("Token [%s] failed: %v", tokens[i], resp.Error)
			}
		}
	}
}

func NotifyLikePush(recipientFCMToken string, likerUsername string, postCaption string) {
	ctx := context.Background()
	client, err := newFirebaseClient(ctx)
	if err != nil {
		log.Printf("Firebase init error: %v", err)
		return
	}

	message := &messaging.Message{
		Token: recipientFCMToken,
		Notification: &messaging.Notification{
			Title: fmt.Sprintf("%s liked your post", likerUsername),
			Body:  postCaption,
		},
		Data: map[string]string{
			"type": "like",
		},
	}

	_, err = client.Send(ctx, message)
	if err != nil {
		log.Printf("Like notification error: %v", err)
	}
}
