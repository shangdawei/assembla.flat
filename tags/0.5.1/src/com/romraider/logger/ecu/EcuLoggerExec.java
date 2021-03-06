/*
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
 */

package com.romraider.logger.ecu;

import com.romraider.Settings;
import com.romraider.swing.LookAndFeelManager;
import com.romraider.util.LogManager;
import com.romraider.util.SettingsManager;
import com.romraider.util.SettingsManagerImpl;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public final class EcuLoggerExec {

    private EcuLoggerExec() {
        throw new UnsupportedOperationException();
    }

    public static void main(String... args) {
        // init debug loging
        LogManager.initDebugLogging();

        // check for dodgy threading - dev only
//        RepaintManager.setCurrentManager(new ThreadCheckingRepaintManager(true));

        // set look and feel
        LookAndFeelManager.initLookAndFeel();

        // load settings
        SettingsManager manager = new SettingsManagerImpl();
        Settings settings = manager.load();

        // start logger
        EcuLogger.startLogger(EXIT_ON_CLOSE, settings, args);
    }

}
