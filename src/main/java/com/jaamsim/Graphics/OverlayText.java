/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2023 JaamSim Software Inc.
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

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.ColourProviders.ColourProvInput;
import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.DisplayModels.TextModel;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.StringProviders.StringProvConstant;
import com.jaamsim.StringProviders.StringProvInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.StringChoiceInput;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.StringListInput;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.TessFontKey;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jogamp.newt.event.KeyEvent;

/**
 * OverylayText displays written text as a 2D overlay on a View window.
 * @author Harry King
 *
 */
public class OverlayText extends OverlayEntity implements TextEntity, EditableText {

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

	@Keyword(description = "The unit type for the numerical value to be displayed as "
	                     + "variable text. Set to DimensionlessUnit if the variable text is "
	                     + "non-numeric, such as the state of a Server.",
	         exampleList = {"DistanceUnit", "DimensionlessUnit"})
	protected final UnitTypeInput unitType;

	@Keyword(description = "The unit in which to express an expression that returns a numeric "
	                     + "value.",
	         exampleList = {"m/s"})
	protected final EntityInput<Unit> unit;

	@Keyword(description = "An expression that returns the variable text to be displayed. "
	                     + "The expression can return a number that will be formated as text, "
	                     + "or it can return text directly, such as the state of a Server. "
	                     + "An object that returns a number, such as a TimeSeries, can also "
	                     + "be entered.",
	         exampleList = {"[Queue1].AverageQueueTime", "[Server1].State",
	                        "'[Queue1].QueueLength + [Queue2].QueueLength'",
	                        "TimeSeries1"})
	protected final StringProvInput dataSource;

	@Keyword(description = "The text to display if there is any failure while formatting the "
	                     + "variable text or while evaluating the expression.",
	         exampleList = {"'Input Error'"})
	protected final StringInput failText;

	@Keyword(description = "The font to be used for the text.",
	         exampleList = { "Arial" })
	private final StringChoiceInput fontName;

	@Keyword(description = "The height of the font as displayed in the view window. Unit is in pixels.",
	         exampleList = {"15"})
	private final SampleInput textHeight;

	@Keyword(description = "The font styles to be applied to the text, e.g. Bold, Italic. ",
	         exampleList = { "Bold" })
	private final StringListInput fontStyle;

	@Keyword(description = "The colour of the text.")
	private final ColourProvInput fontColor;

	@Keyword(description = "If TRUE, then a drop shadow appears for the text.",
	         exampleList = { "TRUE" })
	private final BooleanProvInput dropShadow;

	@Keyword(description = "The colour for the drop shadow.")
	private final ColourProvInput dropShadowColor;

	@Keyword(description = "The { x, y, z } coordinates of the drop shadow's offset, expressed "
	                     + "as a decimal fraction of the text height.",
	         exampleList = { "0.1 -0.1 0.001" })
	private final Vec3dInput dropShadowOffset;

	private String renderText;
	private final EditableTextDelegate editableText;

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(TextModel.class);

		formatText = new StringInput("Format", KEY_INPUTS, "%s");
		formatText.setCallback(formattextCallback);
		this.addInput(formatText);

		unitType = new UnitTypeInput("UnitType", KEY_INPUTS, DimensionlessUnit.class);
		unitType.setCallback(unittypeCallback);
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

		fontName = new StringChoiceInput("FontName", FONT, -1);
		fontName.setChoices(TextModel.validFontNames);
		fontName.setDefaultText("TextModel");
		this.addInput(fontName);

		textHeight = new SampleInput("TextHeight", FONT, 0);
		textHeight.setValidRange(0, 1000);
		textHeight.setIntegerValue(true);
		textHeight.setDefaultText("TextModel");
		this.addInput(textHeight);

		fontColor = new ColourProvInput("FontColour", FONT, ColourInput.BLACK);
		fontColor.setDefaultText("TextModel");
		this.addInput(fontColor);
		this.addSynonym(fontColor, "FontColor");

		fontStyle = new StringListInput("FontStyle", FONT, new ArrayList<String>(0));
		fontStyle.setValidOptions(TextModel.validStyles);
		fontStyle.setCaseSensitive(false);
		fontStyle.setDefaultText("TextModel");
		this.addInput(fontStyle);

		dropShadow = new BooleanProvInput("DropShadow", FONT, false);
		dropShadow.setDefaultText("TextModel");
		this.addInput(dropShadow);

		dropShadowColor = new ColourProvInput("DropShadowColour", FONT, ColourInput.BLACK);
		dropShadowColor.setDefaultText("TextModel");
		this.addInput(dropShadowColor);
		this.addSynonym(dropShadowColor, "DropShadowColor");

		dropShadowOffset = new Vec3dInput("DropShadowOffset", FONT, null);
		dropShadowOffset.setDefaultText("TextModel");
		this.addInput(dropShadowOffset);
	}

	public OverlayText() {
		editableText = new EditableTextDelegate();
	}

	static final InputCallback formattextCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((OverlayText)ent).updateFormattextValue();
		}
	};

	void updateFormattextValue() {
		setText(formatText.getValue());
	}

	static final InputCallback unittypeCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((OverlayText)ent).updateUnitypeValue();
		}
	};

	void updateUnitypeValue() {
		Class<? extends Unit> ut = unitType.getUnitType();
		dataSource.setUnitType(ut);
		unit.setSubClass(ut);
	}

	@Override
	public void setInputsForDragAndDrop() {
		super.setInputsForDragAndDrop();

		// Set the displayed text to the entity's name
		InputAgent.applyArgs(this, "Format", this.getName());
	}

	@Override
	public void setText(String str) {
		editableText.setText(str);
	}

	@Override
	public String getText() {
		return editableText.getText();
	}

	@Override
	public void acceptEdits() {
		editableText.acceptEdits();
		KeywordIndex kw = InputAgent.formatArgs("Format", getText());
		try {
			InputAgent.storeAndExecute(new KeywordCommand(this, kw));
		}
		catch (Exception e) {
			GUIListener gui = getJaamSimModel().getGUIListener();
			if (gui != null)
				gui.invokeErrorDialogBox("Input Error", e.getMessage());
		}
	}

	@Override
	public void cancelEdits() {
		editableText.cancelEdits();
	}

	@Override
	public boolean isEditMode() {
		return editableText.isEditMode();
	}

	@Override
	public void setEditMode(boolean bool) {
		editableText.setEditMode(bool);
	}

	@Override
	public int getInsertPosition() {
		return editableText.getInsertPosition();
	}

	@Override
	public int getNumberSelected() {
		return editableText.getNumberSelected();
	}

	@Override
	public void copyToClipboard() {
		editableText.copyToClipboard();
	}

	@Override
	public void pasteFromClipboard() {
		editableText.pasteFromClipboard();
	}

	@Override
	public void deleteSelection() {
		editableText.deleteSelection();
	}

	@Override
	public int handleEditKeyPressed(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		if (keyChar == '\'') {
			GUIListener gui = getJaamSimModel().getGUIListener();
			if (gui != null)
				gui.invokeErrorDialogBox("Input Error", Input.INP_ERR_QUOTE);
			return CONTINUE_EDITS;
		}
		return editableText.handleEditKeyPressed(keyCode, keyChar, shift, control, alt);
	}

	@Override
	public int handleEditKeyReleased(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		return editableText.handleEditKeyReleased(keyCode, keyChar, shift, control, alt);
	}

	@Override
	public void setInsertPosition(int pos, boolean shift) {
		editableText.setInsertPosition(pos, shift);
	}

	@Override
	public void selectPresentWord() {
		editableText.selectPresentWord();
	}

	@Override
	public boolean handleKeyPressed(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {

		// If F2 is pressed, set edit mode
		if (keyCode == KeyEvent.VK_F2) {
			setEditMode(true);
			RenderManager.redraw();
			return true;
		}

		// If not in edit mode, apply the normal action for the keystroke
		if (!isEditMode()) {
			boolean ret = super.handleKeyPressed(keyCode, keyChar, shift, control, alt);
			return ret;
		}

		// If in edit mode, the apply the keystroke to the text
		int result = handleEditKeyPressed(keyCode, keyChar, shift, control, alt);
		if (result == ACCEPT_EDITS) {
			acceptEdits();
		}
		else if (result == CANCEL_EDITS) {
			cancelEdits();
		}
		RenderManager.redraw();
		return true;
	}

	@Override
	public void handleKeyReleased(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		if (isEditMode()) {
			handleEditKeyReleased(keyCode, keyChar, shift, control, alt);
			return;
		}
		super.handleKeyReleased(keyCode, keyChar, shift, control, alt);
	}

	@Override
	public void handleMouseClicked(short count, int x, int y, int windowWidth, int windowHeight,
			boolean shift, boolean control, boolean alt) {
		if (count > 2)
			return;

		// Double click starts edit mode
		if (!isEditMode() && count == 2) {
			setEditMode(true);
		}

		if (!isEditMode())
			return;

		// Position the insertion point where the text was clicked
		int pos = getStringPosition(x, y, windowWidth, windowHeight);
		editableText.setInsertPosition(pos, shift);

		// Double click selects a whole word
		if (count == 2)
			editableText.selectPresentWord();
	}

	@Override
	public boolean handleDrag(int x, int y, int startX, int startY, int windowWidth, int windowHeight) {
		if (!isEditMode())
			return false;

		// Set the start and end of highlighting
		int insertPos = getStringPosition(x, y, windowWidth, windowHeight);
		int firstPos = getStringPosition(startX, startY, windowWidth, windowHeight);
		editableText.setInsertPosition(insertPos, false);
		editableText.setNumberSelected(firstPos - insertPos);
		return true;
	}

	/**
	 * Returns the insert position in the present text that corresponds to the specified global
	 * coordinate. Index 0 is located immediately before the first character in the text.
	 * @param x - global x-coordinate for the specified position
	 * @param y - global y-coordinate for the specified position
	 * @return insert position in the text string
	 */
	public int getStringPosition(int x, int y, int windowWidth, int windowHeight) {
		double height = getTextHeight(0.0d);
		TessFontKey fontKey = getTessFontKey();
		Vec3d size = RenderManager.inst().getRenderedStringSize(fontKey, height, getText());
		IntegerVector pos = getScreenPosition();
		double startX = getAlignRight() ? windowWidth - pos.get(0) - size.x : pos.get(0);
		double startY = getAlignBottom() ? windowHeight - pos.get(1) - size.y : pos.get(1);
		return RenderManager.inst().getRenderedStringPosition(fontKey, height, getText(), x - startX, -y + startY);
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
			String ret = dataSource.getNextString(this, simTime, siFactor);
			if (ret == null)
				ret = "null";
			return ret;
		}

		// Dynamic text is to be displayed
		try {
			String ret = dataSource.getNextString(this, simTime, formatText.getValue(), siFactor);
			if (ret == null)
				ret = "null";
			return ret;
		}
		catch (Throwable e) {
			return failText.getValue();
		}
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
	public void handleSelectionLost() {
		if (isEditMode()) {
			acceptEdits();
		}
	}

	public String getCachedText() {
		return renderText;
	}

	public TextModel getTextModel() {
		return (TextModel) displayModelListInput.getValue().get(0);
	}

	@Override
	public String getFontName() {
		if (fontName.isDefault()) {
			return getTextModel().getFontName();
		}
		return fontName.getChoice();
	}

	@Override
	public double getTextHeight(double simTime) {
		if (textHeight.isDefault()) {
			return getTextModel().getTextHeightInPixels(simTime);
		}
		return (int) textHeight.getNextSample(this, simTime);
	}

	@Override
	public String getTextHeightString() {
		if (textHeight.isDefault()) {
			return getTextModel().getTextHeightInPixelsString();
		}
		return textHeight.getValueString();
	}

	@Override
	public int getStyle() {
		if (fontStyle.isDefault()) {
			return getTextModel().getStyle();
		}
		return TextModel.getStyle(fontStyle.getValue());
	}

	public TessFontKey getTessFontKey() {
		return new TessFontKey(getFontName(), getStyle());
	}

	@Override
	public Color4d getFontColor(double simTime) {
		if (fontColor.isDefault()) {
			return getTextModel().getFontColor(simTime);
		}
		return fontColor.getNextColour(this, simTime);
	}

	@Override
	public boolean isDropShadow(double simTime) {
		if (dropShadow.isDefault()) {
			return getTextModel().isDropShadow(simTime);
		}
		return dropShadow.getNextBoolean(this, simTime);
	}

	@Override
	public Color4d getDropShadowColor(double simTime) {
		if (dropShadowColor.isDefault()) {
			return getTextModel().getDropShadowColor(simTime);
		}
		return dropShadowColor.getNextColour(this, simTime);
	}

	@Override
	public Vec3d getDropShadowOffset() {
		if (dropShadowOffset.isDefault()) {
			return getTextModel().getDropShadowOffset();
		}
		return dropShadowOffset.getValue();
	}

	@Override
	public boolean isBold() {
		return TextModel.isBold(getStyle());
	}

	@Override
	public boolean isItalic() {
		return TextModel.isItalic(getStyle());
	}

}
