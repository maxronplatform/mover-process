# _Mover Process_

***This dependency is designed for guaranteed processing of event queues in Spring applications. 
If you need to process a lot of events quickly, guaranteed not to lose any, even if your app crashes, then this is the solution for you.***

## Quick start

### Spring Boot

#### 1. Add dependency

*If we use Spring Boot 2 or Spring 5, then we can utilize the Mover Process starter package. 
Let's add the following dependency to the pom.xml file:*

```xml
<dependency>
    <groupId>org.maxron.platform</groupId>
    <artifactId>mover-process</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 2. Enable Mover Process

*To enable Mover Process, Spring makes good use of annotations, much like enabling any other configuration level feature in the framework.
We can enable the Mover Process feature simply by adding the ```@EnableMoverProcess``` annotation to any of the configuration classes:*

```java
@EnableMoverProcess
@SpringBootApplication
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
```
***Note: After we enable Mover Process, for the minimal setup, we must implement ``CommandExecutor.class`` and ``Command.class``.
``CommandExecutor.class`` executes ``Command.class`` events in transaction***

#### 3. Implement Command or extend AbstractCommand

```java
@Getter
public class YourExtendCommand extends AbstractCommand {
    private final YourDataEvent dataEvent;

    public YourExtendCommand(String commandId, String trackingKey, YourDataEvent dataEvent) {
        super(commandId, trackingKey);
        this.dataEvent = dataEvent;
    }
}
```


#### 4. Implement CommandExecutor<T extends Command>

```java
@Component
public class YourCommandExecutor implements CommandExecutor<YourExtendCommand> {
    @Autowired
    private YourMessageChannel messageChannel;
    
    @Override
    public void executeInMoverTransaction(CommandExecution executionContext, TwCommand command) {
        try {
            if (!messageChannel.send(command.getDataEvent())) {
                // re-executing the event
                commandExecution.retry();
            } else {
                // the event was successful, remove it from the queue
                commandExecution.ok();
            }
        } catch (Exception e) {
            if (isTransientException(e)) {
                // re-executing the event
                commandExecution.retry();
            } else {
                // stopping event processing to fix the error
                commandExecution.stop(e);
            }
        }
    }
}
```



