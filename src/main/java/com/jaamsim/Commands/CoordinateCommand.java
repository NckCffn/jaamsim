/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017 JaamSim Software Inc.
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
package com.jaamsim.Commands;

import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.Vec3d;

public class CoordinateCommand extends KeywordCommand {

	private final DisplayEntity dispEnt;
	private final Vec3d globalPos;
	private final ArrayList<Vec3d> globalPts;

	public CoordinateCommand(DisplayEntity ent, KeywordIndex... kws) {
		super(ent, kws);
		dispEnt = ent;
		globalPos = ent.getGlobalPosition();
		ArrayList<Vec3d> pts = ent.getPoints();
		if (pts == null || pts.isEmpty()) {
			globalPts = null;
		}
		else {
			globalPts = ent.getGlobalPosition(pts);
		}
	}

	@Override
	public void execute() {
		super.execute();
		resetPosition();
	}

	@Override
	public void undo() {
		super.undo();
		resetPosition();
	}

	private void resetPosition() {
		Vec3d localPos = dispEnt.getLocalPosition(globalPos);
		KeywordIndex posKw = InputAgent.formatPointInputs("Position", localPos, "m");
		InputAgent.apply(dispEnt, posKw);

		if (globalPts != null) {
			ArrayList<Vec3d> localPts = dispEnt.getLocalPosition(globalPts);
			KeywordIndex ptsKw = InputAgent.formatPointsInputs("Points", localPts, new Vec3d());
			InputAgent.apply(dispEnt, ptsKw);
		}
	}

}
