package com.rs.platform.moverprocess;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Import(MoverProcessConfigurationSelector.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface EnableMoverProcess {
    AdviceMode mode() default AdviceMode.PROXY;
}
