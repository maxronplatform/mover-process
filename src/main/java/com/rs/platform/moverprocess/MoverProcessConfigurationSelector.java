package com.rs.platform.moverprocess;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;

import javax.annotation.Nonnull;

public class MoverProcessConfigurationSelector extends AdviceModeImportSelector<EnableMoverProcess> {
    @Override
    protected String[] selectImports(@Nonnull AdviceMode adviceMode) {
        if (adviceMode == AdviceMode.PROXY) {
            return new String[]{MoverProcessConfiguration.class.getName()};
        }
        return null;
    }
}
