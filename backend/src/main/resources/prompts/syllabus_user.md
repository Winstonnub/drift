Create a 30-item syllabus for this learning topic:

{{topic}}

Return exactly one JSON object with this shape:

{
  "items": [
    {
      "index": 0,
      "title": "Short lesson title",
      "summary": "One sentence explaining what this lesson teaches.",
      "prerequisites": ["Optional prerequisite concept"]
    }
  ]
}

Rules:
- Return exactly 30 items.
- Use indexes 0 through 29 exactly once.
- Keep each title under 80 characters.
- Keep each summary under 180 characters.
- Use an empty prerequisites array when there are no prerequisites.
- Do not include Markdown.
- Do not include code fences.
- Do not include explanations outside the JSON object.