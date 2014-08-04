/**
 * eobjects.org AnalyzerBeans
 * Copyright (C) 2010 eobjects.org
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.analyzer.job;

import org.eobjects.analyzer.job.builder.LazyFilterOutcome;

public final class LazyOutcomeUtils {

    private LazyOutcomeUtils() {
        // prevent instantiation
    }

    public static Outcome load(Outcome outcome) {
        if (outcome instanceof LazyFilterOutcome) {
            LazyFilterOutcome lfo = (LazyFilterOutcome) outcome;
            return new ImmutableFilterOutcome(lfo.getFilterJob(), lfo.getCategory());
        }
        return outcome;
    }

    public static ComponentRequirement load(ComponentRequirement req) {
        // TODO: Consider loading any lazy outcomes nested in the component
        // requirement
        return req;
    }
}
