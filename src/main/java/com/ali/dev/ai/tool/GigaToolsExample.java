package com.ali.dev.ai.tool;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.http.client.HttpClientException;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.moderation.DisabledModerationModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Demonstration of using tools with GigaChat models.
 * <p>
 * This example shows how to extend LLM capabilities by connecting specialized tools to a language model.
 * Key concepts demonstrated:
 * <ul>
 *   <li>Authentication and configuration of GigaChat API connection</li>
 *   <li>Creating an AI assistant with custom tools</li>
 *   <li>Tool descriptions and parameter definitions</li>
 *   <li>Structured output handling</li>
 *   <li>Interactive chat interface</li>
 * </ul>
 *
 * Source project: https://github.com/ai-forever/langchain4j-gigachat/tree/main
 */
public class GigaToolsExample {
    private static final Logger log = LoggerFactory.getLogger(GigaToolsExample.class);

    /**
     * Main entry point for the application.
     *
     * @param args Command line arguments where args[0] should contain GigaChat API authorization key
     */
    public static void main(String[] args) {
        printExamplePrompts();
        String authKey = args[0];

        // Configure GigaChat model with connection parameters
        GigaChatChatModel model = GigaChatChatModel.builder()
                .maxRetries(3) // Maximum retry attempts for failed requests
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_MAX_2) // Specifies which GigaChat model to use
                        .profanityCheck(false) // Disables content filtering
                        .build())
                .verifySslCerts(false) // Bypasses SSL certificate validation (not recommended for production)
                .logRequests(true)    // Enables request logging
                .logResponses(true)  // Enables response logging
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(Scope.GIGACHAT_API_PERS) // Required OAuth scope
                                .authKey(authKey)               // API authorization key
                                .build())
                        .build())
                .build();

        /**
         * Creates an AI Assistant service with:
         * - The configured GigaChat model
         * - Disabled moderation (for demonstration purposes)
         * - Three custom tools (Calculator, Time, and Risk tools)
         */
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model) // Sets the LLM provider
                .moderationModel(new DisabledModerationModel()) // Disables content moderation
                .tools(new CalcTool(), new TimeTool(), new RiskTool()) // Registers custom tools
                .build();

        // Interactive chat loop
        Scanner scanner = new Scanner(System.in);
        log.info("Start Dialog (type 'exit' to exit):");

        try {
            while (true) {
                String userInput = scanner.nextLine();
                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }
                System.out.println(assistant.chat(userInput));
            }
            scanner.close();
        } catch (HttpClientException ex) {
            log.error("code: " + ex.statusCode() + " response:" + ex.bodyAsString());
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Prints example prompts that demonstrate tool usage.
     */
    private static void printExamplePrompts() {
        log.info("what is 3 plus 8");
        log.info("there were 2 apples in the box, then 10 more apples were added, and then 3 apples were stolen twice. how many apples are left in the box in total");
        log.info("how many days until the new year");
        log.info("what was the total damage in the first quarter of 2025");
    }

    /**
     * The Assistant interface defines the AI service contract.
     * <p>
     * Key annotations:
     * - @SystemMessage: Provides the initial system prompt that defines the assistant's behavior
     * - @UserMessage: Marks the method parameter as receiving user input
     *
     * The interface is implemented automatically by LangChain4j's AiServices.
     */
    interface Assistant {
        /**
         * Processes user messages and returns AI responses.
         *
         * @SystemMessage defines the assistant's role and capabilities. This prompt:
         * - Sets the assistant's identity
         * - Instructs it to use available tools
         * - Guides its response behavior
         *
         * @param message The user's input message
         * @return The assistant's response, potentially using tools
         */
        @SystemMessage("You are a assistant who can use tools to answer user questions.")
        String chat(@UserMessage String message);
    }

    /**
     * TimeTool provides current time information to the LLM.
     * <p>
     * Demonstrates how to give LLMs access to real-time data they don't inherently possess.
     */
    static class TimeTool {
        private static final String pattern = "yyyy-MM-dd HH-mm-ss";
        private static final DateTimeFormatter df = DateTimeFormatter.ofPattern(pattern);

        /**
         * Returns current datetime in specified format.
         *
         * @Tool annotation:
         * - Provides the natural language description the LLM uses to understand when to call this tool
         * - The description should clearly explain:
         *   - When to use the tool
         *   - What it does
         *   - The format of returned data
         *
         * @param ignored Required parameter for tool methods (not used)
         * @return CalResult containing formatted datetime string
         */
        @Tool("use this method to get current time, it will return time in format " + pattern)
        CalResult getTime(String ignored) {
            log.info("current time");
            return new CalResult(df.format(LocalDateTime.now()));
        }
    }

    /**
     * RiskTool provides access to simulated risk incident data.
     * <p>
     * Demonstrates how to connect LLMs to structured business data.
     */
    static class RiskTool {
        private static final List<String> risks = List.of(
                "EVE-1|2025-04-11|theft|1000",
                "EVE-2|2025-03-11|flooded|5000",
                "EVE-3|2025-02-11|hardware failure|6000",
                "EVE-4|2025-01-11|PC failure|3000"
        );
        private static final String risksString = makeString().toString();

        /**
         * Returns formatted risk incident data.
         *
         * The tool description includes:
         * - Clear instructions on when to use it
         * - Detailed format specification of:
         *   - Field separator (|)
         *   - Date format
         *   - Field meanings
         *   - Record separator (;)
         *
         * @param ignored Required parameter (not used)
         * @return CalResult containing formatted risk data
         */
        @Tool("use this method to get the list of incidents it will return the list of incidents in the format" +
                "ID|date in yyyy-MM-dd format|reason|lostMoney|;")
        CalResult getRisk(String ignored) {
            log.info("risks list");
            return new CalResult(risksString);
        }

        /**
         * Formats the risk data into a single string.
         */
        private static StringBuilder makeString() {
            StringBuilder sb = new StringBuilder();
            for (String r: risks) {
                sb.append(r);
                sb.append(";");
            }
            return sb;
        }
    }

    /**
     * CalcTool provides mathematical operations.
     * <p>
     * Demonstrates:
     * - Precise calculations (unlike LLM's approximate math)
     * - Parameter annotations
     * - Multiple related operations in one tool class
     */
    static class CalcTool {
        /**
         * Performs addition of two numbers.
         *
         * @P annotations on parameters:
         * - Provide descriptions the LLM uses to understand parameter purposes
         * - Can mark parameters as required
         * - Help the LLM map natural language to method parameters
         *
         * @param value1 First addend (described as "first term")
         * @param value2 Second addend (described as "second term")
         * @return CalResult containing sum
         */
        @Tool("use this method for addition")
        CalResult sum(
                @P(value = "first term", required = true) double value1,
                @P(value = "second term", required = true) double value2) {
            log.info("sum value1: " + value1 + ", value2:" + value2);
            return new CalResult(String.valueOf(value1 + value2));
        }

        /**
         * Performs subtraction of two numbers.
         *
         * @param value1 Minuend (number to subtract from)
         * @param value2 Subtrahend (number to subtract)
         * @return CalResult containing difference
         */
        @Tool("use this method for subtraction")
        CalResult substract(
                @P(value = "number to subtract from", required = true) double value1,
                @P(value = "the number to be subtracted", required = true) double value2) {
            log.info("substract value1: " + value1 + ", value2:" + value2);
            return new CalResult(String.valueOf(value1 - value2));
        }

        /**
         * Performs multiplication of two numbers.
         *
         * @param value1 First factor
         * @param value2 Second factor
         * @return CalResult containing product
         */
        @Tool("use this method for multiplication")
        CalResult mult(
                @P(value = "first multiplier", required = true) double value1,
                @P(value = "second factor", required = true) double value2) {
            log.info("mult value1: " + value1 + ", value2:" + value2);
            return new CalResult(String.valueOf(value1 * value2));
        }
    }

    /**
     * Simple record for structured tool outputs.
     * <p>
     * @Description annotation provides the LLM with information about the field's purpose.
     *
     * @param result The string result from tool execution
     */
    record CalResult(
            @Description("Calculation result") String result) {
    }
}