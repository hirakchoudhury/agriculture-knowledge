"""Builds the admin guide PDF.

The guide is generated rather than hand-made so it can be corrected when the
admin screens change: edit the content below and run this again.

    python scripts/build_admin_guide.py

Output: docs/admin-guide.pdf
"""

from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    KeepTogether,
    ListFlowable,
    ListItem,
    PageBreak,
    PageTemplate,
    Paragraph,
    Preformatted,
    Spacer,
    Table,
    TableStyle,
)

SITE = "https://agriculture-knowledge.vercel.app"

GREEN = colors.HexColor("#2f6b3f")
GREEN_SOFT = colors.HexColor("#eef4ef")
INK = colors.HexColor("#1c1f1d")
MUTED = colors.HexColor("#6b7280")
LINE = colors.HexColor("#d9ded9")

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "admin-guide.pdf"


def styles():
    base = getSampleStyleSheet()
    s = {}
    s["title"] = ParagraphStyle(
        "title", parent=base["Title"], fontName="Helvetica-Bold",
        fontSize=26, leading=30, textColor=INK, alignment=TA_LEFT, spaceAfter=4,
    )
    s["subtitle"] = ParagraphStyle(
        "subtitle", parent=base["Normal"], fontName="Helvetica",
        fontSize=11, leading=16, textColor=MUTED, spaceAfter=18,
    )
    s["h1"] = ParagraphStyle(
        "h1", parent=base["Heading1"], fontName="Helvetica-Bold",
        fontSize=15, leading=19, textColor=GREEN, spaceBefore=14, spaceAfter=6,
        keepWithNext=1,
    )
    s["h2"] = ParagraphStyle(
        "h2", parent=base["Heading2"], fontName="Helvetica-Bold",
        fontSize=11, leading=15, textColor=INK, spaceBefore=10, spaceAfter=3,
        keepWithNext=1,
    )
    s["body"] = ParagraphStyle(
        "body", parent=base["Normal"], fontName="Helvetica",
        fontSize=9.8, leading=13.7, textColor=INK, spaceAfter=5,
    )
    s["bullet"] = ParagraphStyle(
        "bullet", parent=s["body"], spaceAfter=3,
    )
    s["note"] = ParagraphStyle(
        "note", parent=s["body"], fontSize=9.2, leading=13.0, spaceAfter=0,
    )
    s["code"] = ParagraphStyle(
        "code", parent=base["Code"], fontName="Courier",
        fontSize=8.4, leading=11.6, textColor=INK,
        leftIndent=0, firstLineIndent=0,
    )
    s["foot"] = ParagraphStyle(
        "foot", parent=base["Normal"], fontName="Helvetica",
        fontSize=7.6, textColor=MUTED,
    )
    return s


S = styles()


def p(text, style="body"):
    return Paragraph(text, S[style])


def bullets(items, style="bullet"):
    return ListFlowable(
        [ListItem(Paragraph(t, S[style]), leftIndent=12) for t in items],
        bulletType="bullet", bulletFontName="Helvetica", bulletFontSize=9,
        bulletOffsetY=-1, bulletColor=GREEN, leftIndent=12, spaceAfter=6,
    )


def steps(items):
    """Numbered steps. Kept visually distinct from bullets, which are facts."""
    return ListFlowable(
        [ListItem(Paragraph(t, S["bullet"]), leftIndent=16) for t in items],
        bulletType="1", bulletFormat="%s.", bulletFontName="Helvetica-Bold",
        bulletFontSize=9.8, bulletColor=GREEN, leftIndent=16, spaceAfter=6,
    )


def callout(title, text):
    """A tinted box for the things people get wrong if they skim."""
    inner = [p("<b>%s</b>" % title, "note"), Spacer(1, 3), p(text, "note")]
    table = Table([[inner]], colWidths=[158 * mm])
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), GREEN_SOFT),
        ("LINEBEFORE", (0, 0), (0, -1), 2, GREEN),
        ("LEFTPADDING", (0, 0), (-1, -1), 9),
        ("RIGHTPADDING", (0, 0), (-1, -1), 9),
        ("TOPPADDING", (0, 0), (-1, -1), 8),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
    ]))
    return KeepTogether([Spacer(1, 4), table, Spacer(1, 10)])


def code(text):
    box = Table([[Preformatted(text, S["code"])]], colWidths=[158 * mm])
    box.setStyle(TableStyle([
        ("BOX", (0, 0), (-1, -1), 0.6, LINE),
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#fafbfa")),
        ("LEFTPADDING", (0, 0), (-1, -1), 9),
        ("RIGHTPADDING", (0, 0), (-1, -1), 9),
        ("TOPPADDING", (0, 0), (-1, -1), 8),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
    ]))
    return KeepTogether([Spacer(1, 2), box, Spacer(1, 10)])


def table(rows, widths):
    data = [[Paragraph("<b>%s</b>" % c, S["note"]) for c in rows[0]]]
    data += [[Paragraph(c, S["note"]) for c in row] for row in rows[1:]]
    t = Table(data, colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), GREEN_SOFT),
        ("TEXTCOLOR", (0, 0), (-1, 0), GREEN),
        ("LINEBELOW", (0, 0), (-1, 0), 0.8, GREEN),
        ("LINEBELOW", (0, 1), (-1, -2), 0.4, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    return KeepTogether([Spacer(1, 2), t, Spacer(1, 10)])


def decorate(canvas, doc):
    """Header rule and page number. Page 1 gets the rule only."""
    canvas.saveState()
    canvas.setStrokeColor(GREEN)
    canvas.setLineWidth(2)
    canvas.line(26 * mm, A4[1] - 18 * mm, A4[0] - 26 * mm, A4[1] - 18 * mm)

    canvas.setFont("Helvetica", 7.6)
    canvas.setFillColor(MUTED)
    if doc.page > 1:
        canvas.drawString(26 * mm, A4[1] - 15 * mm, "Agriculture Knowledge — Admin guide")
    canvas.drawRightString(A4[0] - 26 * mm, 14 * mm, "Page %d" % doc.page)
    canvas.drawString(26 * mm, 14 * mm, SITE)
    canvas.restoreState()


def story():
    out = []

    # ---------------------------------------------------------------- cover
    out += [
        Spacer(1, 6),
        p("Agriculture Knowledge", "title"),
        p("Admin guide: how to publish content", "subtitle"),
        p(
            "Everything on the site is created and released from the <b>Admin</b> area. "
            "This guide covers the whole route: signing in, setting up exams and topics, "
            "writing an article, adding a YouTube lesson, building a quiz, and putting any "
            "of it in front of readers.",
        ),
        p(
            "One rule runs through all of it: <b>nothing you create is visible until you "
            "publish it</b>. Everything is saved as a draft first, so there is no way to "
            "expose half-finished work by accident.",
        ),
    ]

    out += [p("Signing in", "h1")]
    out += [steps([
        "Go to <b>%s/login</b> and sign in with your admin email and password." % SITE,
        "An <b>Admin</b> link appears in the header once you are signed in. Open it, or go "
        "straight to <b>%s/admin/materials</b>." % SITE,
    ])]
    out += [callout(
        "If the Admin area says &ldquo;Admins only&rdquo;",
        "You are signed in, but that account is not an admin. Admin access is granted by "
        "email on the server, not from inside the site &mdash; ask whoever runs the "
        "deployment to add your address, then sign out and back in.",
    )]

    out += [p("The four tabs", "h1")]
    out += [table(
        [
            ["Tab", "What it is for"],
            ["Dashboard", "Counts of users, material, comments and quiz attempts. A health check, not a place you edit anything."],
            ["Material", "The main workbench. Every article, video and quiz, with its status, and the buttons that publish, unpublish and archive."],
            ["Exams", "The exams learners filter by &mdash; ICAR JRF, IBPS AFO, and so on."],
            ["Topics", "The subject tree. Topics nest, so <i>Soil Science</i> can hold <i>Soil Fertility</i>."],
        ],
        [30 * mm, 128 * mm],
    )]

    out += [p("First: set up exams and topics", "h1")]
    out += [p(
        "Material is tagged with exams and topics, and you can only tick boxes that already "
        "exist. Ten minutes spent here once saves re-editing every article later.",
    )]

    out += [p("Adding an exam", "h2")]
    out += [steps([
        "Open the <b>Exams</b> tab.",
        "Type the exam name &mdash; for example <b>ICAR JRF</b> &mdash; and, if you like, a "
        "one-line description.",
        "Press <b>Add</b>. The web address is generated from the name.",
    ])]

    # ListFlowable does not honour keepWithNext from the heading above it, so this
    # heading has to be tied to its steps by hand.
    out += [KeepTogether([
        p("Adding a topic", "h2"),
        steps([
            "Open the <b>Topics</b> tab.",
            "Type the topic name, for example <b>Soil Science</b>.",
            "Leave <b>Parent topic</b> empty for a top-level subject. To make a sub-topic, "
            "choose its parent &mdash; <i>Soil Fertility</i> under <i>Soil Science</i>.",
            "Press <b>Add</b>.",
        ]),
    ])]
    out += [callout(
        "Topics are shared, exams are lists",
        "The same topic tree serves every exam. Tag an article with the topic it is about "
        "and every exam it is relevant to &mdash; one article on soil pH can belong to "
        "several exams at once. You are not creating a copy per exam.",
    )]

    out += [p("Publishing an article", "h1")]
    out += [steps([
        "<b>Material</b> tab &rarr; <b>New article</b>.",
        "<b>Title.</b> Required. This becomes the heading and the web address.",
        "<b>Summary.</b> One or two sentences. It is what readers see on the home page, on "
        "cards and in search results, so write it for someone deciding whether to click.",
        "<b>Level.</b> Beginner, Intermediate or Advanced.",
        "<b>Topics</b> and <b>Exams.</b> Tick as many as apply. Both may be left empty &mdash; "
        "see the note below.",
        "<b>Body.</b> Write in the editor. The toolbar gives you Bold, Italic, two heading "
        "levels, bullet and numbered lists, quotes, code and links.",
        "Press <b>Save draft</b>. You land back on the Material list with the article "
        "marked <b>DRAFT</b>.",
        "When it reads the way you want, press <b>Publish</b> on that row.",
    ])]
    out += [callout(
        "You can publish before you have decided on tags",
        "Topics and exams are both optional. An article with no tags still publishes and "
        "still appears on the home page &mdash; it simply will not show up under an exam or "
        "topic filter. Add the tags whenever you are ready; the article does not need to be "
        "unpublished first.",
    )]
    out += [p(
        "Formatting is cleaned against a safe list when you save, so pasted text from Word "
        "or a web page may lose styling the editor cannot express. The words always survive; "
        "check the result after pasting a long passage.",
    )]

    out += [p("Adding a video lesson", "h1")]
    out += [steps([
        "<b>Material</b> tab &rarr; <b>New video</b>.",
        "Fill in title, summary, level, topics and exams exactly as for an article.",
        "Paste the <b>YouTube link</b>. Any shape works &mdash; a normal watch link, a "
        "youtu.be short link, an embed link, a Shorts link, or the bare eleven-character "
        "video id.",
        "The player appears below the box as soon as the link is understood. <b>Watch that "
        "preview</b>: it is the video your readers will get.",
        "Press <b>Save draft</b>, then <b>Publish</b> from the Material list.",
    ])]
    out += [callout(
        "&ldquo;That does not look like a YouTube link yet&rdquo;",
        "The Save button stays disabled until a valid video is recognised. It is almost "
        "always a truncated copy or a link to a channel or playlist rather than a single "
        "video. Open the video on YouTube, use Share &rarr; Copy link, and paste that.",
    )]

    out += [p("Building a quiz", "h1")]
    out += [p(
        "A quiz is created in two stages: the settings first, then the questions. A quiz with "
        "no questions cannot be attempted, so the site takes you straight from one to the other.",
    )]

    out += [p("Stage one &mdash; the settings", "h2")]
    out += [steps([
        "<b>Material</b> tab &rarr; <b>New quiz</b>.",
        "Title, summary, level, topics and exams, as before.",
        "<b>Pass mark (%).</b> The percentage a learner needs to pass. 60 is the default.",
        "<b>Time limit.</b> In minutes. Tick <b>No time limit</b> for untimed practice.",
        "<b>Shuffle</b> presents the questions in a different order to each learner.",
        "Press <b>Create and add questions</b>.",
    ])]

    out += [p("Stage two &mdash; the questions", "h2")]
    out += [p(
        "<b>Add a question</b> gives you one blank question at a time: the question text, the "
        "options, a tick on the correct one, marks, optional negative marks, and an optional "
        "explanation shown after the attempt.",
    )]
    out += [p(
        "For a whole set at once, press <b>Paste in many</b> and paste plain text in this shape:",
    )]
    out += [code(
        "Q: Which nutrient is most affected by soil pH?\n"
        "*Phosphorus\n"
        "Carbon\n"
        "Silicon\n"
        "E: Availability drops sharply outside pH 6 to 7.\n"
        "\n"
        "Q: What does CEC stand for?\n"
        "*Cation exchange capacity\n"
        "Crop establishment coefficient"
    )]
    out += [bullets([
        "A <b>blank line</b> separates one question from the next.",
        "An <b>asterisk</b> marks the correct option. Every question needs at least two "
        "options and exactly one asterisk.",
        "The <b>E:</b> line is the explanation, and is optional.",
        "Marks default to 1 and can be edited afterwards in the form.",
    ])]
    out += [p(
        "Press <b>Add these</b>. Any problems are listed all at once rather than one at a "
        "time, so fix them together and paste again. Imported questions join the form, where "
        "you can still edit them &mdash; then press <b>Save</b>.",
    )]
    out += [callout(
        "Save, then publish",
        "Saving the questions does not publish the quiz. Go back to <b>Material</b> and press "
        "<b>Publish</b> on the quiz row. <b>Export as text</b> on the question screen gives "
        "you the same paste format back &mdash; a quick way to keep a backup or reuse a set.",
    )]

    out += [p("Publish, unpublish, archive", "h1")]
    out += [p(
        "Every row on the Material list carries a status. Use the two filters at the top of "
        "the list &mdash; status and format &mdash; to find things quickly once there is a lot here.",
    )]
    out += [table(
        [
            ["Status", "Who can see it", "How you get there"],
            ["DRAFT", "Only admins.", "Everything starts here. <b>Unpublish</b> returns a live item to draft."],
            ["PUBLISHED", "Everyone, including signed-out visitors.", "<b>Publish</b> on the row."],
            ["ARCHIVED", "Nobody. It is hidden but not deleted.", "<b>Archive</b> on the row."],
        ],
        [26 * mm, 62 * mm, 70 * mm],
    )]
    out += [callout(
        "Archive rather than delete",
        "Archiving keeps the row, so the likes, comments and quiz attempts that point at it "
        "stay intact. Use <b>Unpublish</b> for something you are fixing and will put back; "
        "use <b>Archive</b> for something that is finished with.",
    )]

    out += [p("Before you press Publish", "h1")]
    out += [bullets([
        "The <b>summary</b> is filled in &mdash; it is the first thing a reader sees.",
        "The <b>level</b> matches the content.",
        "At least one <b>topic</b> and one <b>exam</b> are ticked, unless you deliberately "
        "mean to leave it untagged for now.",
        "For a video: the <b>preview plays the right video</b>.",
        "For a quiz: the questions are <b>saved</b>, each has exactly one correct answer, and "
        "the pass mark and time limit are what you intended.",
        "For an article: any pasted text still reads correctly after the formatting clean-up, "
        "and links open the right pages.",
    ])]

    out += [p("Common questions", "h1")]
    out += [p("<b>I published something by mistake.</b>", "h2")]
    out += [p("Press <b>Unpublish</b> on that row. It returns to draft immediately.")]

    out += [p("<b>Can I edit something that is already published?</b>", "h2")]
    out += [p(
        "Yes. Publishing is a status, not a lock. For a large rewrite, unpublish first so "
        "readers do not meet the work in progress.",
    )]

    out += [p("<b>The topic or exam I want is not in the list.</b>", "h2")]
    out += [p(
        "Add it on the <b>Topics</b> or <b>Exams</b> tab, then reopen the material form &mdash; "
        "the lists are loaded when the form opens, so a form left sitting open will not show "
        "something added since.",
    )]

    out += [p("<b>Nothing appears on the home page.</b>", "h2")]
    out += [p(
        "The home page shows published material only. Set the status filter on the Material "
        "tab to <b>Drafts</b> to see what is waiting.",
    )]

    return out


def build():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    doc = BaseDocTemplate(
        str(OUT), pagesize=A4,
        leftMargin=26 * mm, rightMargin=26 * mm,
        topMargin=26 * mm, bottomMargin=22 * mm,
        title="Agriculture Knowledge - Admin guide",
        author="Agriculture Knowledge",
        subject="How to publish articles, videos and quizzes",
    )
    frame = Frame(
        doc.leftMargin, doc.bottomMargin, doc.width, doc.height, id="body",
        leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0,
    )
    doc.addPageTemplates([PageTemplate(id="main", frames=[frame], onPage=decorate)])
    doc.build(story())
    print("Wrote %s" % OUT)


if __name__ == "__main__":
    build()
