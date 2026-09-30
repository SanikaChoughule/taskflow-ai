package com.taskflowai.ai

class AIServiceFactory(
    private val localAiService: LocalRuleBasedAIServiceImpl = LocalRuleBasedAIServiceImpl()
) {
    fun getService(providerName: String): AIService {
        return when (providerName.uppercase()) {
            "GEMINI" -> GeminiAIServiceImpl(localAiService)
            "OPENAI" -> OpenAIAIServiceImpl(localAiService)
            else -> localAiService
        }
    }
}
