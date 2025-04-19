# GigaChat AI Tools Integration

A Java application demonstrating GigaChat AI integration with custom tools for calculations, time retrieval, and risk data management.

Based on https://github.com/ai-forever/langchain4j-gigachat

## 📌 Features
- Integration with GigaChat API via `langchain4j-gigachat`
- Three custom tools:
  - `CalcTool`: Basic arithmetic operations (addition, subtraction, multiplication)
  - `TimeTool`: Current time retrieval
  - `RiskTool`: Sample risk incident data
- Interactive console interface

## 🚀 Quick Start

### Prerequisites
- Java 17+
- GigaChat API key (get free key [here](https://developers.sber.ru/studio))

### Installation
1. Clone the repository:
```bash
git clone https://github.com/AndreyAA/GigaToolExample.git
cd GigaToolExample
```

2. Build with Maven:
```bash
mvn clean package
```

3. Run the application:
```bash
java -jar target/GigaToolExample.jar YOUR_API_KEY
```

## 🛠️ Usage Examples

Try these commands in the interactive chat:
```
what is 15 plus 23
what time is it now
show me recent risks
calculate 45 minus 12
how many days until new year
```

## 🔧 Custom Tools

### 1. Calculator Tool
```java
@Tool("Performs mathematical operations")
public CalResult sum(double value1, double value2) {
    return new CalResult(String.valueOf(value1 + value2));
}
```

### 2. Time Tool
```java
@Tool("Gets current time")
public CalResult getTime() {
    return new CalResult(df.format(LocalDateTime.now()));
}
```

### 3. Risk Tool
```java
@Tool("Provides risk incident data")
public CalResult getRisk() {
    return new CalResult(risksString);
}
```

## ⚙️ Configuration

Default GigaChat settings (modifiable in code):
```java
GigaChatChatModel.builder()
    .modelName(ModelName.GIGA_CHAT_MAX_2)
    .profanityCheck(false)
    .maxRetries(3)
    .logRequests(true)
    .logResponses(true)
    .build();
```

## 🤝 Contributing
Pull requests are welcome! For major changes, please open an issue first.

## 📜 License
[MIT](https://choosealicense.com/licenses/mit/)

---

