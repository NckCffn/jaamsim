/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.Samples;

import java.util.ArrayList;
import java.util.Collections;

import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.StringVector;

public class SampleInput extends Input<SampleProvider> {
	private Class<? extends Unit> unitType;

	public SampleInput(String key, String cat, SampleProvider def) {
		super(key, cat, def);
		unitType = null;
	}

	public void setUnitType(Class<? extends Unit> u) {
		unitType = u;
	}

	@Override
	public void parse(StringVector input)
	throws InputErrorException {
		Input.assertCountRange(input, 1, 2);

		// Try to parse as a constant value
		try {
			DoubleVector tmp = Input.parseDoubles(input, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);
			value = new ConstantDouble(unitType, tmp.get(0));
			this.updateEditingFlags();
			return;
		}
		catch (InputErrorException e) {}

		// If not a constant, try parsing a SampleProvider
		Entity ent = Input.parseEntity(input.get(0), Entity.class);
		SampleProvider s = Input.castImplements(ent, SampleProvider.class);
		if( s.getUnitType() != UserSpecifiedUnit.class )
			Input.assertUnitsMatch(unitType, s.getUnitType());
		value = s;
		this.updateEditingFlags();
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<String>();
		for (Entity each: Entity.getAll()) {
			if( (SampleProvider.class).isAssignableFrom(each.getClass()) ) {
			    list.add(each.getInputName());
			}
		}
		Collections.sort(list);
		return list;
	}

	public void verifyUnit() {
		Input.assertUnitsMatch( unitType, value.getUnitType());
	}
}
