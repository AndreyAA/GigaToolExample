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
 * There are several simple tools using GigaChat models.
 * source project https://github.com/ai-forever/langchain4j-gigachat/tree/main
 */
public class GigaToolsExample {
    private static final Logger log = LoggerFactory.getLogger(GigaToolsExample.class);

    public static void main(String[] args) {
        printExamplePromts();
        String authKey = args[0];
        GigaChatChatModel model = GigaChatChatModel.builder()
                .maxRetries(3)
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_MAX_2)
                        .profanityCheck(false)
                        .build())
                .verifySslCerts(false)
                .logRequests(true)
                .logResponses(true)
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(Scope.GIGACHAT_API_PERS)
                                .authKey(authKey)
                                .build())
                        .build())
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .moderationModel(new DisabledModerationModel())
//                .tools(new CalcTool())
                .tools(new CalcTool(), new TimeTool(), new RiskTool())
                .build();

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

    private static void printExamplePromts() {
        log.info("what is 3 plus 8");
        log.info("there were 2 apples in the box, then 10 more apples were added, and then 3 apples were stolen twice. how many apples are left in the box in total");
        log.info("how many days until the new year");
        log.info("what was the total damage in the first quarter of 2025");
    }

    interface Assistant {
        @SystemMessage("You are a assistant who can use tools to answer user questions.")
        String chat(@UserMessage String message);
    }

    static class TimeTool {

        private static final String pattern = "yyyy-MM-dd HH-mm-ss";
        private static final DateTimeFormatter df = DateTimeFormatter.ofPattern(pattern);
        @Tool("use this method to get current time, it will return time in format " + pattern)
        CalResult getTime(String ignored) {
            log.info("current time");
            return new CalResult(df.format(LocalDateTime.now()));
        }

    }

    static class RiskTool {
        private static final List<String> risks = List.of(
                "EVE-1|2025-04-11|theft|1000",
                "EVE-2|2025-03-11|flooded|5000",
                "EVE-3|2025-02-11|hardware failure|6000",
                "EVE-4|2025-01-11|PC failure|3000"
        );
        private static final String risksString = makeString().toString();

        @Tool("use this method to get the list of incidents it will return the list of incidents in the format" +
                "ID|дата в формате yyyy-MM-dd|reason|lostMoney|;")
        CalResult getRisk(String ignored) {
            log.info("risks list");
            return new CalResult(risksString);
        }

        private static StringBuilder makeString() {
            StringBuilder sb = new StringBuilder();
            for (String r: risks) {
                sb.append(r);
                sb.append(";");
            }
            return sb;
        }

    }

    static class CalcTool {

        @Tool("use this method for addition")
        CalResult sum(
                @P(value = "first term", required = true) double value1,
                @P(value = "second term", required = true) double value2) {

            log.info("sum value1: " + value1 + ", value2:" + value2);
            return new CalResult(String.valueOf(value1 + value2));
        }


        @Tool("use this method for subtraction")
        CalResult substract(
                @P(value = "number to subtract from", required = true) double value1,
                @P(value = "the number to be subtracted", required = true) double value2) {

            log.info("substract value1: " + value1 + ", value2:" + value2);
            return new CalResult(String.valueOf(value1 - value2));
        }


        @Tool("use this method for multiplication")
        CalResult mult(
                @P(value = "first multiplier", required = true) double value1,
                @P(value = "second factor", required = true) double value2) {

            log.info("mult value1: " + value1 + ", value2:" + value2);
            return new CalResult(String.valueOf(value1 * value2));
        }
    }

    record CalResult(
            @Description("Result") String result) {
    }
}
