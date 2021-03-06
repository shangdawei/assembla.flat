package com.romraider.logger.ecu.ui.swing.menubar.action;

import com.romraider.logger.ecu.EcuLogger;
import java.awt.event.ActionEvent;

public final class DisconnectAction extends AbstractAction {

    public DisconnectAction(EcuLogger logger) {
        super(logger);
    }

    public void actionPerformed(ActionEvent actionEvent) {
        try {
            logger.stopLogging();
        } catch (Exception e) {
            logger.reportError(e);
        }
    }
}
