package com.drift.llm;

import java.util.List;

public record AnthropicMessageResponse(

    List<AnthropicContentBlock> content

) {
}