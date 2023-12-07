/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2013 Ausenco Engineering Canada Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.Graphics;

import com.jaamsim.Samples.SampleListInput;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.UserSpecifiedUnit;

public class XYGraph extends AbstractGraph {

	// Key Inputs category

	@Keyword(description = "One or more sources of data to be graphed on the primary y-axis.\n"
	                     + "Each source is graphed as a separate line or bar and is specified by an Entity and its Output.",
	         exampleList = {"{ Entity1 Output1 } { Entity2 Output2 }"})
	protected final SampleListInput dataSource;

	@Keyword(description = "Set to TRUE if the primary series are to be shown as bars instead of lines.")
	protected final BooleanInput showBars;

	@Keyword(description = "One or more sources of data to be graphed on the secondary y-axis.\n"
	                     + "Each source is graphed as a separate line or bar and is specified by an Entity and its Output.",
	         exampleList = {"{ Entity1 Output1 } { Entity2 Output2 }"})
	protected final SampleListInput secondaryDataSource;

	@Keyword(description = "Set to TRUE if the secondary series are to be shown as bars instead of lines.")
	protected final BooleanInput secondaryShowBars;

	{
		// Key Inputs category

		dataSource = new SampleListInput("DataSource", KEY_INPUTS, null);
		dataSource.setUnitType(UserSpecifiedUnit.class);
		this.addInput(dataSource);

		showBars = new BooleanInput("ShowBars", KEY_INPUTS, false);
		this.addInput(showBars);

		secondaryDataSource = new SampleListInput("SecondaryDataSource", KEY_INPUTS, null);
		secondaryDataSource.setUnitType(UserSpecifiedUnit.class);
		this.addInput(secondaryDataSource);

		secondaryShowBars = new BooleanInput("SecondaryShowBars", KEY_INPUTS, false);
		this.addInput(secondaryShowBars);
	}

	public XYGraph() {}

	@Override
	public void setInputsForDragAndDrop() {}

}
