/*
 *
 * RomRaider Open-Source Tuning, Logging and Reflashing
 * Copyright (C) 2006-2008 RomRaider.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package com.romraider;

import static com.romraider.ECUEditorManager.getECUEditor;
import static com.romraider.logger.ecu.EcuLogger.startLogger;
import static com.romraider.swing.LookAndFeelManager.initLookAndFeel;
import static com.romraider.util.LogManager.initDebugLogging;
import com.romraider.util.SettingsManager;
import com.romraider.util.SettingsManagerImpl;
import org.apache.log4j.Logger;
import static org.apache.log4j.Logger.getLogger;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import java.io.File;

public class ECUExec {
    private static final Logger LOGGER = getLogger(ECUExec.class);
    private static final String START_LOGGER_ARG = "-logger";

    private ECUExec() {
        throw new UnsupportedOperationException();
    }

    public static void main(String args[]) {
        // init debug logging
        initDebugLogging();

        // check for dodgy threading - dev only
//        RepaintManager.setCurrentManager(new ThreadCheckingRepaintManager(true));

        // set look and feel
        initLookAndFeel();

        // open editor or logger
        if (containsLoggerArg(args)) openLogger();
        else openEditor(args);
    }

    private static boolean containsLoggerArg(String[] args) {
        if (args.length == 0) return false;
        return args[0].equals(START_LOGGER_ARG);
    }

    private static void openLogger() {
        SettingsManager manager = new SettingsManagerImpl();
        Settings settings = manager.load();
        startLogger(EXIT_ON_CLOSE, settings);
    }

    private static void openEditor(String[] args) {
        ECUEditor editor = getECUEditor();
        if (args.length > 0) openRom(editor, args[0]);
    }

    private static void openRom(final ECUEditor editor, final String rom) {
        invokeLater(new Runnable() {
            public void run() {
                try {
                    File file = new File(rom);
                    editor.openImage(file);
                } catch (Exception ex) {
                    LOGGER.error("Error opening rom", ex);
                }
            }
        });
    }
}