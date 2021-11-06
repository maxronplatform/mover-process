# _The Mover Process application._

***The dependency is designed for guaranteed processing of event queues in Spring applications. If you need to process 100% of events quickly without losing any, then this is the solution for you.***

## Quick start

### Spring Boot

#### 1. Add dependency

*If we use Spring Boot 2 or Spring 5, then we can utilize the mover-process starter package. Let's add the following dependency to the pom.xml file:*

```xml
<dependency>
    <groupId>com.rs.platform</groupId>
    <artifactId>mover-process</artifactId>
    <version>1.0.0-SNAPSHOT</version>
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
***Note: After we enable Mover Process, for the minimal setup, we must implements ``CommandExecutor.class`` and ``Command.class``. 
A MoverProcessCommand is an event object that the CommandExecutor interacts with.***

#### 3. Implements Command or extends AbstractCommand

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


#### 4. Implements CommandExecutor<T extends Command>

```java
@Component
public class YourCommandExecutor implements CommandExecutor<YourExtendCommand> {
    @Autowired
    private YourMessageChannel messageChannel;
    
    @Override
    public void executeInMoverTransaction(CommandExecution executionContext, TwCommand command) {
        // example -> 
        try {
            if (!messageChannel.send(command.getDataEvent())) {
                
                commandExecution.retry();
            } else {
                
                commandExecution.ok();
            }
        } catch (Exception e) {
            if (isTransientException(e)) {
                
                commandExecution.retry();

            } else {

                commandExecution.stop(e);
            }
        }
    }
}
```



