# Быстрый старт

## Запуск за 5 минут

### Шаг 1: Проверка требований

```bash
# Проверка версии Java (требуется 21+)
java -version

# Проверка Maven
mvn -version
```

### Шаг 2: Клонирование и сборка

```bash
# Клонировать репозиторий
git clone https://github.com/yourusername/solar-performance-analyzer.git
cd solar-performance-analyzer

# Собрать проект
mvn clean package
```

### Шаг 3: Запуск приложения

#### Windows

```cmd
run.bat
```

#### Linux/Mac

```bash
chmod +x run.sh
./run.sh
```

### Шаг 4: Просмотр результатов

Приложение автоматически:
1. Откроет JavaFX дашборд
2. Начнет загрузку солнечных изображений каждые 5 секунд
3. Покажет сравнение GPU vs CPU обработки
4. Отобразит метрики производительности в реальном времени

## Что вы увидите

### Дашборд

```
┌─────────────────────────────────────────────────────┐
│  Solar Performance Analyzer - Real-Time GPU vs CPU  │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐         ┌──────────────┐        │
│  │ GPU Processing│         │CPU Processing│        │
│  │  [Image]     │         │  [Image]     │        │
│  │   8.5 ms     │         │   520 ms     │        │
│  └──────────────┘         └──────────────┘        │
│                                                     │
├─────────────────────────────────────────────────────┤
│  Performance Chart                                  │
│  [График задержки GPU vs CPU во времени]           │
├─────────────────────────────────────────────────────┤
│  Frames: 42 | FPS: 60.0 | Avg Latency: 264 ms     │
│  GPU Speedup: 61.2x (GPU: 8.5ms, CPU: 520ms)      │
│  Throughput: 0.2 frames/sec | Total Data: 21.5 MB │
└─────────────────────────────────────────────────────┘
```

## Устранение неполадок

### Проблема: Ошибка компиляции

```bash
# Очистить и пересобрать
mvn clean install -U
```

### Проблема: GPU не обнаружен

Приложение автоматически переключится на CPU. Вы увидите:
```
WARN - GPU not available, using CPU fallback
```

Это нормально! Приложение будет работать, но медленнее.

### Проблема: Ошибка загрузки изображений

Проверьте интернет-соединение. Приложение автоматически:
1. Попробует основной URL
2. Переключится на резервный URL
3. Повторит попытку через 5 секунд

### Проблема: JavaFX не запускается

Убедитесь, что используете Java 21 с JavaFX:
```bash
java --version
# Должно показать OpenJDK 21 или выше
```

## Следующие шаги

1. **Изучите код**: Начните с `SolarAnalyzerApp.java`
2. **Запустите тесты**: `mvn test`
3. **Запустите бенчмарки**: `mvn clean package && java -jar target/benchmarks.jar`
4. **Прочитайте документацию**: См. `docs/ARCHITECTURE_RU.md`

## Быстрые команды

```bash
# Сборка без тестов
mvn clean package -DskipTests

# Запуск с профилированием JFR
java -XX:StartFlightRecording=filename=recording.jfr \
     --enable-preview \
     -jar target/solar-performance-analyzer-1.0.0.jar

# Запуск тестов
mvn test

# Запуск с отладкой
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
     --enable-preview \
     -jar target/solar-performance-analyzer-1.0.0.jar
```

## Настройка производительности

### Для максимальной производительности

```bash
java --enable-preview \
     --add-modules jdk.incubator.vector \
     -XX:+UseZGC \
     -XX:+ZGenerational \
     -XX:MaxGCPauseMillis=1 \
     -XX:+UnlockExperimentalVMOptions \
     -XX:+UseNUMA \
     -XX:+AlwaysPreTouch \
     -Xms4g -Xmx4g \
     -jar target/solar-performance-analyzer-1.0.0.jar
```

### Для отладки

```bash
java --enable-preview \
     --add-modules jdk.incubator.vector \
     -Xms512m -Xmx2g \
     -XX:+PrintGCDetails \
     -jar target/solar-performance-analyzer-1.0.0.jar
```

## Получение помощи

- **Документация**: См. папку `docs/`
- **Issues**: https://github.com/yourusername/solar-performance-analyzer/issues
- **Email**: [ваш-email]

---

**Готовы к глубокому погружению? Читайте полную документацию в `README_RU.md`**
