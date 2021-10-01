/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2021 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.StringProviders.StringProvConstant;
import com.jaamsim.StringProviders.StringProvInput;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.Unit;

/**
 * The "Text" object displays written text within the 3D model universe.  Both fixed and variable text can be displayed.
 * @author Harry King
 *
 */
public class Text extends TextBasics {

	@Keyword(description = "The fixed and variable text to be displayed, as specified by a Java "
	                     + "format string. "
	                     + "If variable text is to be displayed using the DataSource keyword, "
	                     + "include the appropriate Java format in the text. "
	                     + "For example, %s will display a text output and %.6f will display a "
	                     + "number with six decimal digits of accuracy. "
	                     + "A new line can be started by entering %n. "
	                     + "Note that a % character is generated by entering %%.",
	         exampleList = {"'Present speed = %.3f m/s'", "'Present State = %s'",
	                        "'First line%nSecond line'"})
	protected final StringInput formatText;

	@Keyword(description = "The unit type for the numerical value to be displayed as variable "
	                     + "text. Set to DimensionlessUnit if the variable text is non-numeric "
	                     + "such as the state of a Server.",
	         exampleList = {"DistanceUnit", "DimensionlessUnit"})
	protected final UnitTypeInput unitType;

	@Keyword(description = "The unit in which to express an expression that returns a numerical "
	                     + "value. For example, if the UnitType input has been set to "
	                     + "DistanceUnit, then the output value could be displayed in kilometres, "
	                     + "instead of meters, by entering km to this keyword.",
	         exampleList = {"m/s"})
	protected final EntityInput<Unit> unit;

	@Keyword(description = "An expression that returns the variable text to be displayed. "
	                     + "The expression can return a number that will be formated as text, "
	                     + "or it can return text directly, such as the state of a Server.",
	         exampleList = {"[Queue1].AverageQueueTime", "[Server1].State",
	                        "'[Queue1].QueueLength + [Queue2].QueueLength'",
	                        "TimeSeries1"})
	protected final StringProvInput dataSource;

	@Keyword(description = "The text to display if there is any failure while formatting the "
	                     + "variable text or while evaluating the expression.",
	         exampleList = {"'Input Error'"})
	private final StringInput failText;

	@Keyword(description = "If TRUE, then the size of the background rectangle is adjusted "
	                     + "whenever the displayed text or its font is changed. "
	                     + "Resizing is performed only when the DataSource input has been left "
	                     + "blank.",
	         exampleList = { "TRUE" })
	protected final BooleanInput autoSize;

	protected String renderText = "";

	{
		formatText = new StringInput("Format", KEY_INPUTS, "%s");
		this.addInput(formatText);

		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, DimensionlessUnit.class);
		this.addInput(unitType);

		unit = new EntityInput<>(Unit.class, "Unit", KEY_INPUTS, null);
		unit.setSubClass(null);
		this.addInput(unit);

		dataSource = new StringProvInput("DataSource", KEY_INPUTS, new StringProvConstant(""));
		dataSource.setUnitType(DimensionlessUnit.class);
		this.addInput(dataSource);
		this.addSynonym(dataSource, "OutputName");

		failText = new StringInput("FailText", KEY_INPUTS, "Input Error");
		this.addInput(failText);

		autoSize = new BooleanInput("AutoSize", OPTIONS, true);
		this.addInput(autoSize);
	}

	public Text() {}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == formatText) {
			setText(formatText.getValue());
			return;
		}

		if (in == unitType) {
			Class<? extends Unit> ut = unitType.getUnitType();
			dataSource.setUnitType(ut);
			unit.setSubClass(ut);
			return;
		}
	}

	@Override
	public void setInputsForDragAndDrop() {
		super.setInputsForDragAndDrop();

		// Set the displayed text to the entity's name
		InputAgent.applyArgs(this, "Format", this.getName());

		// Set the size to match the text
		resizeForText();
	}

	@Override
	public void acceptEdits() {
		super.acceptEdits();
		ArrayList<KeywordIndex> kwList = new ArrayList<>(2);
		kwList.add( InputAgent.formatArgs("Format", getText()) );
		if (isAutoSize()) {
			Vec3d size = getAutoSize(getText(), getStyle(), getTextHeight());
			kwList.add( getJaamSimModel().formatVec3dInput("Size", size, DistanceUnit.class) );
		}
		KeywordIndex[] kws = new KeywordIndex[kwList.size()];
		kwList.toArray(kws);
		try {
			InputAgent.storeAndExecute(new KeywordCommand(this, kws));
		}
		catch (Exception e) {
			GUIListener gui = getJaamSimModel().getGUIListener();
			if (gui != null)
				gui.invokeErrorDialogBox("Input Error", e.getMessage());
		}
	}

	public String getRenderText(double simTime) {

		// If the object is selected, show the editable text
		if (isEditMode())
			return getText();

		double siFactor = 1.0d;
		if (unit.getValue() != null)
			siFactor = unit.getValue().getConversionFactorToSI();

		// Default Format
		if (formatText.isDefault()) {
			String ret = dataSource.getValue().getNextString(simTime, siFactor);
			if (ret == null)
				ret = "null";
			return ret;
		}

		// Dynamic text is to be displayed
		try {
			String ret = dataSource.getValue().getNextString(simTime, formatText.getValue(), siFactor);
			if (ret == null)
				ret = "null";
			return ret;
		}
		catch (Throwable e) {
			return failText.getValue();
		}
	}

	public boolean isAutoSize() {
		return autoSize.getValue() && dataSource.isDefault();
	}

	@Override
	public void updateGraphics(double simTime) {
		super.updateGraphics(simTime);

		// This text is cached because reflection is used to get it, so who knows how long it will take
		String newRenderText = getRenderText(simTime);
		if (newRenderText.equals(renderText)) {
			// Nothing important has changed
			return;
		}

		// The text has been updated
		renderText = newRenderText;
	}

	@Override
	public String getCachedText() {
		return renderText;
	}

}
