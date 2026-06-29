package com.drift.email;

import com.drift.lessons.Lesson;
import com.drift.tracks.Track;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class LessonEmailRenderer {

    public String renderHtml(Track track, Lesson lesson, String lessonUrl) {
        String escapedTopic = HtmlUtils.htmlEscape(track.getTopic()); // escape speical html characters like <>
        String escapedTitle = HtmlUtils.htmlEscape(lesson.getTitle());
        String escapedMarkdown = HtmlUtils.htmlEscape(lesson.getContentMarkdown());
        String escapedLessonUrl = HtmlUtils.htmlEscape(lessonUrl);

        return """
            <!doctype html>
            <html>
              <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #111827;">
                <main style="max-width: 680px; margin: 0 auto; padding: 24px;">
                  <p style="color: #6b7280; font-size: 14px; margin: 0 0 8px;">
                    Drift lesson for %s
                  </p>
                  <h1 style="font-size: 24px; margin: 0 0 16px;">%s</h1>
                  <div style="white-space: pre-wrap; font-size: 16px;">%s</div>
                  <p style="margin-top: 24px;">
                    <a href="%s" style="color: #2563eb;">Open in Drift</a>
                  </p>
                </main>
              </body>
            </html>
            """.formatted(
                escapedTopic,
                escapedTitle,
                escapedMarkdown,
                escapedLessonUrl
            );
    }

    public String renderText(Track track, Lesson lesson, String lessonUrl) {
        return """
            Drift lesson for %s

            %s

            %s

            Open in Drift:
            %s
            """.formatted(
                track.getTopic(),
                lesson.getTitle(),
                lesson.getContentMarkdown(),
                lessonUrl
            );
    }

}