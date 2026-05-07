package handlers

import (
	"context"
    "log"
    "firebase.google.com/go/v4"
    "firebase.google.com/go/v4/messaging"
    "google.golang.org/api/option"
)

func NotifyGroupMembersPush(groupIDs []int, authorID int, postDescription string) {
    ctx := context.Background()

    // 1. Initialisation du SDK avec votre fichier JSON
    opt := option.WithServiceAccountFile("locusKey.json")
    app, err := firebase.NewApp(ctx, nil, opt)
    if err != nil {
        log.Printf("Erreur initialisation Firebase: %v", err)
        return
    }

    client, err := app.Messaging(ctx)
    if err != nil {
        log.Printf("Erreur client Messaging: %v", err)
        return
    }

    // 2. Récupération des tokens FCM dans la DB (exclure l'auteur)
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

    message := &messaging.MulticastMessage{
        Tokens: tokens,
        Notification: &messaging.Notification{
            Title: "Locus : Nouveau post !",
            Body:  postDescription,
        },
        Data: map[string]string{
            "click_action": "FLUTTER_NOTIFICATION_CLICK", 
            "type":         "new_post",
        },
    }

    // 4. Envoi
    br, err := client.SendEachForMulticast(ctx, message)
    if err != nil {
        log.Printf("Erreur critique Firebase : %v", err)
    } else {
        log.Printf("%d notifications envoyées, %d échecs", br.SuccessCount, br.FailureCount)
    
    // AJOUTEZ CECI POUR VOIR L'ERREUR DÉTAILLÉE PAR JETON
    for i, resp := range br.Responses {
        if !resp.Success {
            log.Printf("Échec pour le token [%s] : %v", tokens[i], resp.Error)
        }
    }
}
}