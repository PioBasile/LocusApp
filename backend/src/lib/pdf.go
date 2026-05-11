package lib
 
import (
	"bytes"
	"fmt"
	"strings"
)
 
// ─────────────────────────────────────────────────────────────────────────────
// Minimal PDF 1.4 builder — texte seul, police Helvetica, encodage WinAnsi.
// Aucune dépendance externe. Suffisant pour streamer un itinéraire en PDF.
//
// Limites assumées :
//   - Texte uniquement (pas d'images, pas de vectoriel).
//   - Une seule police (Helvetica + bold via /Helvetica-Bold).
//   - Encodage WinAnsi (≈ CP1252) — couvre l'ensemble du français usuel ;
//     les caractères hors charset sont remplacés par '?'.
//   - Pas de retour à la ligne automatique : les lignes trop longues sont
//     tronquées (à charge de l'appelant de découper).
// ─────────────────────────────────────────────────────────────────────────────
 
type pdfTextOp struct {
	X, Y    float64
	Size    float64
	Font    string // "F1" (Helvetica) ou "F2" (Helvetica-Bold)
	Content []byte // déjà encodé WinAnsi + échappé
}
 
type pdfPage struct {
	ops []pdfTextOp
}
 
// PDFBuilder construit un document PDF page par page.
type PDFBuilder struct {
	pages         []*pdfPage
	cur           *pdfPage
	pageWidth     float64
	pageHeight    float64
}
 
// NewPDF démarre un nouveau document A4 portrait (595 × 842 pt).
func NewPDF() *PDFBuilder {
	return &PDFBuilder{pageWidth: 595, pageHeight: 842}
}
 
// NewPage ouvre une nouvelle page.
func (p *PDFBuilder) NewPage() {
	pg := &pdfPage{}
	p.pages = append(p.pages, pg)
	p.cur = pg
}
 
// Text ajoute une ligne de texte en (x, y) — y mesuré depuis le bas de la page.
func (p *PDFBuilder) Text(x, y, size float64, s string) {
	p.addText(x, y, size, "F1", s)
}
 
// TextBold idem, en gras.
func (p *PDFBuilder) TextBold(x, y, size float64, s string) {
	p.addText(x, y, size, "F2", s)
}
 
func (p *PDFBuilder) addText(x, y, size float64, font, s string) {
	if p.cur == nil {
		p.NewPage()
	}
	p.cur.ops = append(p.cur.ops, pdfTextOp{
		X: x, Y: y, Size: size, Font: font,
		Content: pdfEscapeString(toWinAnsi(s)),
	})
}
 
// PageHeight expose la hauteur courante (utile pour positionner depuis le haut).
func (p *PDFBuilder) PageHeight() float64 { return p.pageHeight }
 
// Bytes sérialise le PDF complet.
func (p *PDFBuilder) Bytes() []byte {
	if len(p.pages) == 0 {
		p.NewPage()
	}
 
	var buf bytes.Buffer
	buf.WriteString("%PDF-1.4\n%\xff\xff\xff\xff\n")
 
	offsets := []int{} // offset de chaque obj indexé à partir de 1
 
	addObj := func(content string) {
		offsets = append(offsets, buf.Len())
		buf.WriteString(content)
	}
 
	// Plan des objets :
	//   1 : Catalog
	//   2 : Pages tree
	//   3 : Font Helvetica (F1)
	//   4 : Font Helvetica-Bold (F2)
	//   Pour chaque page i (1..N) :
	//     obj (4 + 2i - 1) : Page
	//     obj (4 + 2i    ) : Content stream
	firstPageObj := 5
 
	addObj("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
 
	// Construction de la liste de Kids pour les Pages
	pageRefs := make([]string, len(p.pages))
	for i := range p.pages {
		pageRefs[i] = fmt.Sprintf("%d 0 R", firstPageObj+2*i)
	}
	addObj(fmt.Sprintf("2 0 obj\n<< /Type /Pages /Kids [%s] /Count %d >>\nendobj\n",
		strings.Join(pageRefs, " "), len(p.pages)))
 
	addObj("3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n")
	addObj("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n")
 
	for i, pg := range p.pages {
		pageObjNum := firstPageObj + 2*i
		contentObjNum := pageObjNum + 1
 
		// Page
		pageDict := fmt.Sprintf(
			"%d 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.0f %.0f] /Contents %d 0 R /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> >>\nendobj\n",
			pageObjNum, p.pageWidth, p.pageHeight, contentObjNum)
		addObj(pageDict)
 
		// Content stream
		var stream bytes.Buffer
		for _, op := range pg.ops {
			fmt.Fprintf(&stream, "BT /%s %.2f Tf %.2f %.2f Td (", op.Font, op.Size, op.X, op.Y)
			stream.Write(op.Content)
			stream.WriteString(") Tj ET\n")
		}
		streamBytes := stream.Bytes()
		var obj bytes.Buffer
		fmt.Fprintf(&obj, "%d 0 obj\n<< /Length %d >>\nstream\n", contentObjNum, len(streamBytes))
		obj.Write(streamBytes)
		obj.WriteString("\nendstream\nendobj\n")
		addObj(obj.String())
	}
 
	// xref
	xrefOffset := buf.Len()
	fmt.Fprintf(&buf, "xref\n0 %d\n", len(offsets)+1)
	buf.WriteString("0000000000 65535 f \n")
	for _, o := range offsets {
		fmt.Fprintf(&buf, "%010d 00000 n \n", o)
	}
	fmt.Fprintf(&buf, "trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n",
		len(offsets)+1, xrefOffset)
 
	return buf.Bytes()
}
 
// ─────────────────────────────────────────────────────────────────────────────
// Helpers d'encodage
// ─────────────────────────────────────────────────────────────────────────────
 
// Table de conversion Unicode → WinAnsi (CP1252).
// Couvre tout l'ASCII (identité) + jeu français complet + quelques ponctuations
// typographiques courantes.
var winAnsiMap = map[rune]byte{
	'À': 0xC0, 'Á': 0xC1, 'Â': 0xC2, 'Ã': 0xC3, 'Ä': 0xC4, 'Å': 0xC5, 'Æ': 0xC6,
	'Ç': 0xC7,
	'È': 0xC8, 'É': 0xC9, 'Ê': 0xCA, 'Ë': 0xCB,
	'Ì': 0xCC, 'Í': 0xCD, 'Î': 0xCE, 'Ï': 0xCF,
	'Ñ': 0xD1,
	'Ò': 0xD2, 'Ó': 0xD3, 'Ô': 0xD4, 'Õ': 0xD5, 'Ö': 0xD6,
	'Ù': 0xD9, 'Ú': 0xDA, 'Û': 0xDB, 'Ü': 0xDC,
	'à': 0xE0, 'á': 0xE1, 'â': 0xE2, 'ã': 0xE3, 'ä': 0xE4, 'å': 0xE5, 'æ': 0xE6,
	'ç': 0xE7,
	'è': 0xE8, 'é': 0xE9, 'ê': 0xEA, 'ë': 0xEB,
	'ì': 0xEC, 'í': 0xED, 'î': 0xEE, 'ï': 0xEF,
	'ñ': 0xF1,
	'ò': 0xF2, 'ó': 0xF3, 'ô': 0xF4, 'õ': 0xF5, 'ö': 0xF6,
	'ù': 0xF9, 'ú': 0xFA, 'û': 0xFB, 'ü': 0xFC,
	'ÿ': 0xFF,
	'Œ': 0x8C, 'œ': 0x9C, 'Ÿ': 0x9F,
	'€': 0x80, '«': 0xAB, '»': 0xBB, '°': 0xB0,
	'\u2019': 0x27, // ’
	'\u2018': 0x27, // ‘
	'\u201C': 0x22, '\u201D': 0x22, // “ ”
	'\u2013': 0x2D, '\u2014': 0x2D, // – —
	'\u2026': 0x85,                 // …
	'\u00A0': 0x20,                 // nbsp
}
 
func toWinAnsi(s string) []byte {
	out := make([]byte, 0, len(s))
	for _, r := range s {
		switch {
		case r < 0x80:
			out = append(out, byte(r))
		default:
			if b, ok := winAnsiMap[r]; ok {
				out = append(out, b)
			} else {
				out = append(out, '?')
			}
		}
	}
	return out
}
 
// pdfEscapeString échappe les bytes spéciaux d'une PDF literal string : '(', ')', '\\'.
func pdfEscapeString(b []byte) []byte {
	out := make([]byte, 0, len(b)+8)
	for _, c := range b {
		switch c {
		case '(', ')', '\\':
			out = append(out, '\\', c)
		case '\n':
			out = append(out, '\\', 'n')
		case '\r':
			out = append(out, '\\', 'r')
		default:
			out = append(out, c)
		}
	}
	return out
}