# API Документация

## Обзор

Solar Performance Analyzer предоставляет программный API для интеграции в другие приложения.

## Основные компоненты

### SolarDataPipeline

Главный класс для управления конвейером обработки данных.

```java
import com.solarlab.core.SolarDataPipeline;

// Создание конвейера
SolarDataPipeline pipeline = new SolarDataPipeline();

// Запуск обработки
pipeline.start();

// Подписка на поток данных
pipeline.getDataStream()
    .subscribe(processedImage -> {
        // Обработка изображения
        byte[] gpuData = processedImage.getGpuData();
        byte[] cpuData = processedImage.getCpuData();
        
        System.out.println("GPU время: " + processedImage.getGpuProcessingTime() + "ns");
        System.out.println("CPU время: " + processedImage.getCpuProcessingTime() + "ns");
        System.out.println("Ускорение: " + processedImage.getSpeedup() + "x");
    });

// Остановка
pipeline.shutdown();
```

### ProcessedImage

Контейнер для обработанных изображений с метриками.

```java
public class ProcessedImage {
    // Получить данные GPU обработки
    public byte[] getGpuData();
    
    // Получить данные CPU обработки
    public byte[] getCpuData();
    
    // Получить лучший результат (GPU если доступен)
    public byte[] getData();
    
    // Время обработки GPU (наносекунды)
    public long getGpuProcessingTime();
    
    // Время обработки CPU (наносекунды)
    public long getCpuProcessingTime();
    
    // Коэффициент ускорения GPU vs CPU
    public double getSpeedup();
    
    // Размер оригинального изображения
    public int getOriginalSize();
}
```

### PerformanceMetrics

Lock-free сборщик метрик производительности.

```java
import com.solarlab.benchmark.PerformanceMetrics;

PerformanceMetrics metrics = pipeline.getMetrics();

// Средняя задержка обработки (миллисекунды)
double avgLatency = metrics.getAverageLatency();

// Средняя задержка GPU (миллисекунды)
double gpuLatency = metrics.getGpuAverageLatency();

// Средняя задержка CPU (миллисекунды)
double cpuLatency = metrics.getCpuAverageLatency();

// Коэффициент ускорения
double speedup = metrics.getSpeedup();

// Пропускная способность (кадры/сек)
double throughput = metrics.getThroughput();

// Общее количество кадров
long totalFrames = metrics.getTotalFrames();

// Общий объем данных (байты)
long totalBytes = metrics.getTotalBytes();

// Минимальная задержка
long minLatency = metrics.getMinLatency();

// Максимальная задержка
long maxLatency = metrics.getMaxLatency();

// Сброс метрик
metrics.reset();
```

### GpuImageProcessor

GPU-ускоренная обработка изображений.

```java
import com.solarlab.gpu.GpuImageProcessor;
import io.netty.buffer.ByteBuf;

GpuImageProcessor processor = new GpuImageProcessor();

// Проверка доступности GPU
boolean hasGpu = processor.isGpuAvailable();

// Обработка изображения
byte[] processed = processor.process(byteBuf);

// Очистка ресурсов
processor.shutdown();
```

### CpuImageProcessor

CPU обработка с SIMD оптимизацией.

```java
import com.solarlab.core.CpuImageProcessor;

CpuImageProcessor processor = new CpuImageProcessor();

// Обработка изображения
byte[] processed = processor.process(byteBuf);
```

### NettyImageDownloader

Высокопроизводительная загрузка изображений.

```java
import com.solarlab.netty.NettyImageDownloader;
import reactor.core.publisher.Mono;

NettyImageDownloader downloader = new NettyImageDownloader();

// Загрузка изображения
Mono<ByteBuf> imageMono = downloader.download(
    "https://sohowww.nascom.nasa.gov/data/realtime/eit_195/1024/latest.jpg"
);

imageMono.subscribe(
    byteBuf -> {
        // Обработка данных
        int size = byteBuf.readableBytes();
        System.out.println("Загружено: " + size + " байт");
        
        // Не забудьте освободить буфер!
        byteBuf.release();
    },
    error -> System.err.println("Ошибка: " + error.getMessage())
);

// Очистка
downloader.shutdown();
```

### FlareDetector

Обнаружение солнечных вспышек.

```java
import com.solarlab.core.FlareDetector;
import java.util.List;

FlareDetector detector = new FlareDetector();

// Обнаружение вспышек
List<FlareDetector.FlareEvent> flares = detector.detectFlares(imageData);

for (FlareDetector.FlareEvent flare : flares) {
    System.out.println("Вспышка обнаружена:");
    System.out.println("  Позиция: (" + flare.x + ", " + flare.y + ")");
    System.out.println("  Размер: " + flare.size + " пикселей");
    System.out.println("  Интенсивность: " + flare.intensity);
}
```

## Примеры использования

### Пример 1: Простая обработка

```java
import com.solarlab.core.SolarDataPipeline;

public class SimpleExample {
    public static void main(String[] args) {
        SolarDataPipeline pipeline = new SolarDataPipeline();
        
        pipeline.start();
        
        pipeline.getDataStream()
            .take(10) // Обработать 10 изображений
            .subscribe(
                image -> System.out.println("Ускорение: " + image.getSpeedup() + "x"),
                error -> System.err.println("Ошибка: " + error),
                () -> {
                    System.out.println("Завершено!");
                    pipeline.shutdown();
                }
            );
        
        // Ждем завершения
        try {
            Thread.sleep(60000); // 60 секунд
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

### Пример 2: Сохранение изображений

```java
import com.solarlab.core.SolarDataPipeline;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SaveImagesExample {
    public static void main(String[] args) throws Exception {
        SolarDataPipeline pipeline = new SolarDataPipeline();
        pipeline.start();
        
        pipeline.getDataStream()
            .take(5)
            .subscribe(image -> {
                try {
                    // Сохранить GPU результат
                    Files.write(
                        Paths.get("gpu_" + System.currentTimeMillis() + ".jpg"),
                        image.getGpuData()
                    );
                    
                    // Сохранить CPU результат
                    Files.write(
                        Paths.get("cpu_" + System.currentTimeMillis() + ".jpg"),
                        image.getCpuData()
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        
        Thread.sleep(30000);
        pipeline.shutdown();
    }
}
```

### Пример 3: Мониторинг производительности

```java
import com.solarlab.core.SolarDataPipeline;
import com.solarlab.benchmark.PerformanceMetrics;

public class MonitoringExample {
    public static void main(String[] args) throws Exception {
        SolarDataPipeline pipeline = new SolarDataPipeline();
        PerformanceMetrics metrics = pipeline.getMetrics();
        
        pipeline.start();
        
        // Периодический вывод метрик
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10000); // Каждые 10 секунд
                    
                    System.out.println("\n=== Метрики производительности ===");
                    System.out.println("Кадров обработано: " + metrics.getTotalFrames());
                    System.out.println("Средняя задержка: " + metrics.getAverageLatency() + " мс");
                    System.out.println("GPU задержка: " + metrics.getGpuAverageLatency() + " мс");
                    System.out.println("CPU задержка: " + metrics.getCpuAverageLatency() + " мс");
                    System.out.println("Ускорение: " + metrics.getSpeedup() + "x");
                    System.out.println("Пропускная способность: " + metrics.getThroughput() + " fps");
                    System.out.println("Данных загружено: " + 
                        (metrics.getTotalBytes() / 1024 / 1024) + " МБ");
                    
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
        
        Thread.sleep(60000);
        pipeline.shutdown();
    }
}
```

### Пример 4: Обнаружение вспышек

```java
import com.solarlab.core.SolarDataPipeline;
import com.solarlab.core.FlareDetector;

public class FlareDetectionExample {
    public static void main(String[] args) throws Exception {
        SolarDataPipeline pipeline = new SolarDataPipeline();
        FlareDetector detector = new FlareDetector();
        
        pipeline.start();
        
        pipeline.getDataStream()
            .subscribe(image -> {
                // Обнаружение вспышек на GPU изображении
                var flares = detector.detectFlares(image.getGpuData());
                
                if (!flares.isEmpty()) {
                    System.out.println("\n⚠️ ОБНАРУЖЕНЫ СОЛНЕЧНЫЕ ВСПЫШКИ!");
                    for (var flare : flares) {
                        System.out.println(flare);
                    }
                }
            });
        
        Thread.sleep(300000); // 5 минут
        pipeline.shutdown();
    }
}
```

## Реактивное программирование

Приложение использует Project Reactor для неблокирующей обработки.

### Базовые операторы

```java
// Ограничение количества элементов
pipeline.getDataStream()
    .take(10)
    .subscribe(...);

// Фильтрация
pipeline.getDataStream()
    .filter(image -> image.getSpeedup() > 50)
    .subscribe(...);

// Преобразование
pipeline.getDataStream()
    .map(image -> image.getSpeedup())
    .subscribe(speedup -> System.out.println("Ускорение: " + speedup));

// Группировка
pipeline.getDataStream()
    .buffer(Duration.ofSeconds(30))
    .subscribe(images -> {
        System.out.println("Обработано " + images.size() + " изображений за 30 сек");
    });
```

## Обработка ошибок

```java
pipeline.getDataStream()
    .subscribe(
        image -> {
            // Успешная обработка
        },
        error -> {
            // Обработка ошибки
            System.err.println("Ошибка: " + error.getMessage());
        },
        () -> {
            // Завершение потока
            System.out.println("Поток завершен");
        }
    );
```

## Конфигурация

### Изменение интервала обновления

Отредактируйте `SolarDataPipeline.java`:

```java
private static final Duration UPDATE_INTERVAL = Duration.ofSeconds(5);
// Измените на нужный интервал
```

### Изменение URL источника

```java
private static final String PRIMARY_URL = "ваш-url";
private static final String FALLBACK_URL = "резервный-url";
```

## Лучшие практики

1. **Всегда освобождайте ByteBuf**: 
   ```java
   byteBuf.release();
   ```

2. **Используйте try-with-resources**:
   ```java
   try (SolarDataPipeline pipeline = new SolarDataPipeline()) {
       pipeline.start();
       // ...
   }
   ```

3. **Обрабатывайте ошибки**:
   ```java
   .subscribe(
       data -> {},
       error -> logger.error("Error", error)
   );
   ```

4. **Мониторьте метрики**:
   ```java
   PerformanceMetrics metrics = pipeline.getMetrics();
   // Регулярно проверяйте метрики
   ```

## Ограничения

- Максимальный размер изображения: 4096x4096
- Максимальная частота обновления: 1 раз в секунду
- Пул соединений: 10 соединений
- Таймаут загрузки: 10 секунд

## Поддержка

Для вопросов по API:
- GitHub Issues: https://github.com/yourusername/solar-performance-analyzer/issues
- Email: [ваш-email]
