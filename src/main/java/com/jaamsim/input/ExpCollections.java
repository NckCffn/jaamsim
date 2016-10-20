package com.jaamsim.input;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.ExpResult.Iterator;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class ExpCollections {

	public static boolean isCollectionClass(Class<?> klass) {
		if (Map.class.isAssignableFrom(klass)) {
			return true;
		}
		if (List.class.isAssignableFrom(klass)) {
			return true;
		}
		if (DoubleVector.class.isAssignableFrom(klass)) {
			return true;
		}
		if (IntegerVector.class.isAssignableFrom(klass)) {
			return true;
		}
		if (klass.isArray()) {
			return true;
		}

		return false;
	}


	public static ExpResult getCollection(Object obj, Class<? extends Unit> ut) {
		if (obj instanceof Map) {
			MapCollection col = new MapCollection((Map<?,?>)obj, ut);
			return ExpResult.makeCollectionResult(col);
		}

		if (obj instanceof List) {
			ListCollection col = new ListCollection((List<?>)obj, ut);
			return ExpResult.makeCollectionResult(col);
		}

		if (obj.getClass().isArray()) {
			ArrayCollection col = new ArrayCollection(obj, ut);
			return ExpResult.makeCollectionResult(col);
		}

		if (obj instanceof DoubleVector) {
			DoubleVectorCollection col = new DoubleVectorCollection((DoubleVector)obj, ut);
			return ExpResult.makeCollectionResult(col);
		}

		if (obj instanceof IntegerVector) {
			IntegerVectorCollection col = new IntegerVectorCollection((IntegerVector)obj, ut);
			return ExpResult.makeCollectionResult(col);
		}

		assert false;
		return null;
	}

	public static ExpResult makeExpressionCollection(ArrayList<ExpResult> vals) {
		return ExpResult.makeCollectionResult(new AssignableArrayCollection(vals));
	}

	private static class ListCollection implements ExpResult.Collection {

		private static class Iter implements ExpResult.Iterator {

			private int next = 0;
			private final List<?> list;
			public Iter(List<?> l) {
				this.list = l;
			}

			@Override
			public boolean hasNext() {
				return next < list.size();
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				ExpResult ret = ExpResult.makeNumResult(next + 1, DimensionlessUnit.class);
				next++;
				return ret;
			}
		}

		@Override
		public Iterator getIter() {
			return new Iter(list);
		}

		private final List<?> list;
		private final Class<? extends Unit> unitType;

		public ListCollection(List<?> l, Class<? extends Unit> ut) {
			this.list = l;
			this.unitType = ut;
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {
			if (index.type != ExpResType.NUMBER) {
				throw new ExpError(null, 0, "ArrayList  is not being indexed by a number");
			}

			int indexVal = (int)index.value - 1; // Expressions use 1-base arrays

			if (indexVal >= list.size()  || indexVal < 0) {
				return ExpResult.makeNumResult(0, unitType); // TODO: Is this how we want to handle this case?
			}
			Object val = list.get(indexVal);

			return ExpEvaluator.getResultFromObject(val, unitType);
		}

		@Override
		public int getSize() {
			return list.size();
		}

		@Override
		public void assign(ExpResult key, ExpResult value) throws ExpError {
			throw new ExpError(null, 0, "Can not assign to built in collection");
		}

		@Override
		public String getOutputString() {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("{ ");
				for (int i = 0; i < list.size(); ++i) {
					ExpResult val = index(ExpResult.makeNumResult(i+1, DimensionlessUnit.class));
					sb.append(val.getOutputString());
					if (i < list.size() -1) {
						sb.append(", ");
					}
				}
				sb.append("}");
				return sb.toString();

			} catch (ExpError err) {
				return String.format("An error occurred: %s", err.getMessage());
			}
		}
	}

	private static class ArrayCollection implements ExpResult.Collection {

		private final Object array;
		private final Class<? extends Unit> unitType;

		public ArrayCollection(Object a, Class<? extends Unit> ut) {
			this.array = a;
			this.unitType = ut;
		}

		private static class Iter implements ExpResult.Iterator {

			private int next = 0;
			private final Object array;

			public Iter(Object a) {
				array = a;
			}

			@Override
			public boolean hasNext() {
				return next < Array.getLength(array);
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				ExpResult ret = ExpResult.makeNumResult(next + 1, DimensionlessUnit.class);
				next++;
				return ret;
			}
		}

		@Override
		public Iterator getIter() {
			return new Iter(array);
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {
			if (index.type != ExpResType.NUMBER) {
				throw new ExpError(null, 0, "ArrayList  is not being indexed by a number");
			}

			int indexVal = (int)index.value - 1;

			int length = Array.getLength(array);

			if (indexVal >= length  || indexVal < 0) {
				return ExpResult.makeNumResult(0, unitType); // TODO: Is this how we want to handle this case?
			}

			Class<?> componentClass = array.getClass().getComponentType();

			if (!componentClass.isPrimitive()) {
				// This is an object type so we can use the rest of the reflection system as usual
				Object val = Array.get(array, indexVal);
				return ExpEvaluator.getResultFromObject(val, unitType);
			}

			if (    componentClass == Double.TYPE ||
			        componentClass == Float.TYPE ||
			        componentClass == Long.TYPE ||
			        componentClass == Integer.TYPE ||
			        componentClass == Short.TYPE ||
			        componentClass == Byte.TYPE ||
			        componentClass == Character.TYPE) {
				// This is a numeric type and should be convertible to double
				return ExpResult.makeNumResult(Array.getDouble(array, indexVal), unitType);
			}
			if (componentClass == Boolean.TYPE) {
				// Convert boolean to 1 or 0
				double val = (Array.getBoolean(array, indexVal)) ? 1.0 : 0.0;
				return ExpResult.makeNumResult(val, unitType);
			}
			throw new ExpError(null, 0, "Unknown type in array");
		}
		@Override
		public int getSize() {
			return Array.getLength(array);
		}
		@Override
		public void assign(ExpResult key, ExpResult value) throws ExpError {
			throw new ExpError(null, 0, "Can not assign to built in collection");
		}

		@Override
		public String getOutputString() {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("{ ");
				for (int i = 0; i < Array.getLength(array); ++i) {
					ExpResult val = index(ExpResult.makeNumResult(i+1, DimensionlessUnit.class));
					sb.append(val.getOutputString());
					if (i < Array.getLength(array) -1) {
						sb.append(", ");
					}
				}
				sb.append("}");
				return sb.toString();

			} catch (ExpError err) {
				return String.format("An error occurred: %s", err.getMessage());
			}
		}

	}

	private static class DoubleVectorCollection implements ExpResult.Collection {

		private final DoubleVector vector;
		private final Class<? extends Unit> unitType;

		public DoubleVectorCollection(DoubleVector v, Class<? extends Unit> ut) {
			this.vector = v;
			this.unitType = ut;
		}

		private static class Iter implements ExpResult.Iterator {

			private int next = 0;
			private final DoubleVector vector;

			public Iter(DoubleVector v) {
				this.vector = v;
			}
			@Override
			public boolean hasNext() {
				return next < vector.size();
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				ExpResult ret = ExpResult.makeNumResult(next + 1, DimensionlessUnit.class);
				next++;
				return ret;
			}
		}
		@Override
		public Iterator getIter() {
			return new Iter(vector);
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {

			if (index.type != ExpResType.NUMBER) {
				throw new ExpError(null, 0, "DoubleVector is not being indexed by a number");
			}

			int indexVal = (int)index.value - 1; // Expressions use 1-base arrays

			if (indexVal >= vector.size() || indexVal < 0) {
				return ExpResult.makeNumResult(0, unitType); // TODO: Is this how we want to handle this case?
			}

			Double value = vector.get(indexVal);
			return ExpResult.makeNumResult(value, unitType);
		}
		@Override
		public int getSize() {
			return vector.size();
		}
		@Override
		public void assign(ExpResult key, ExpResult value) throws ExpError {
			throw new ExpError(null, 0, "Can not assign to built in collection");
		}

		@Override
		public String getOutputString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{ ");
			for (int i = 0; i < vector.size(); ++i) {
				sb.append(vector.get(i+1)*Unit.getDisplayedUnitFactor(unitType));
				sb.append(" ");
				sb.append(Unit.getDisplayedUnit(unitType));
				if (i < vector.size()) {
					sb.append(", ");
				}
			}
			sb.append("}");
			return sb.toString();
		}

	}

	private static class IntegerVectorCollection implements ExpResult.Collection {

		private final IntegerVector vector;
		private final Class<? extends Unit> unitType;

		public IntegerVectorCollection(IntegerVector v, Class<? extends Unit> ut) {
			this.vector = v;
			this.unitType = ut;
		}

		private class Iter implements ExpResult.Iterator {

			private int next = 0;
			private final IntegerVector vector;

			public Iter(IntegerVector v) {
				this.vector = v;
			}

			@Override
			public boolean hasNext() {
				return next < vector.size();
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				ExpResult ret = ExpResult.makeNumResult(next + 1, DimensionlessUnit.class);
				next++;
				return ret;
			}
		}
		@Override
		public Iterator getIter() {
			return new Iter(vector);
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {

			if (index.type != ExpResType.NUMBER) {
				throw new ExpError(null, 0, "IntegerVector is not being indexed by a number");
			}

			int indexVal = (int)index.value - 1; // Expressions use 1-base arrays

			if (indexVal >= vector.size() || indexVal < 0) {
				return ExpResult.makeNumResult(0, unitType); // TODO: Is this how we want to handle this case?
			}

			Integer value = vector.get(indexVal);
			return ExpResult.makeNumResult(value, unitType);
		}
		@Override
		public int getSize() {
			return vector.size();
		}
		@Override
		public void assign(ExpResult key, ExpResult value) throws ExpError {
			throw new ExpError(null, 0, "Can not assign to built in collection");
		}

		@Override
		public String getOutputString() {
			StringBuilder sb = new StringBuilder();
			sb.append("{ ");
			for (int i = 0; i < vector.size(); ++i) {
				sb.append(vector.get(i+1)*Unit.getDisplayedUnitFactor(unitType));
				sb.append(" ");
				sb.append(Unit.getDisplayedUnit(unitType));
				if (i < vector.size()) {
					sb.append(", ");
				}
			}
			sb.append("}");
			return sb.toString();
		}

	}

	private static class MapCollection implements ExpResult.Collection {

		private final Map<?,?> map;
		private final Class<? extends Unit> unitType;

		public MapCollection(Map<?,?> m, Class<? extends Unit> ut) {
			this.map = m;
			this.unitType = ut;
		}

		private static class Iter implements ExpResult.Iterator {

			java.util.Iterator<?> keySetIt;
			public Iter(Map<?,?> map) {
				keySetIt = map.keySet().iterator();
			}

			@Override
			public boolean hasNext() {
				return keySetIt.hasNext();
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				Object mapKey = keySetIt.next();

				return ExpEvaluator.getResultFromObject(mapKey, DimensionlessUnit.class);
			}
		}
		@Override
		public Iterator getIter() {
			return new Iter(map);
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {
			Object key;
			switch (index.type) {
			case ENTITY:
				key = index.entVal;
				break;
			case NUMBER:
				key = Double.valueOf(index.value);
				break;
			case STRING:
				key = index.stringVal;
				break;
			case COLLECTION:
				throw new ExpError(null, 0, "Can not index with a collection");
			default:
				assert(false);
				key = null;
				break;
			}
			Object val = map.get(key);
			if (val == null) {
				return ExpResult.makeNumResult(0, unitType); // TODO: Is this how we want to handle this case?
			}
			return ExpEvaluator.getResultFromObject(val, unitType);
		}
		@Override
		public int getSize() {
			return map.size();
		}
		@Override
		public void assign(ExpResult key, ExpResult value) throws ExpError {
			throw new ExpError(null, 0, "Can not assign to built in collection");
		}
		@Override
		public String getOutputString() {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("{ ");
				Iterator it = getIter();
				while(it.hasNext()) {
					ExpResult index = it.nextKey();
					sb.append(index.getOutputString());
					sb.append(" = ");
					sb.append(index(index).getOutputString());
					if (it.hasNext()) {
						sb.append(", ");
					}
				}
				sb.append("}");
				return sb.toString();

			} catch (ExpError err) {
				return String.format("An error occurred: %s", err.getMessage());
			}
		}

	}
	private static class AssignableArrayCollection implements ExpResult.Collection {

		private final ArrayList<ExpResult> list;

		public AssignableArrayCollection(ArrayList<ExpResult> vals) {
			list = new ArrayList<>(vals);
		}

		@Override
		public ExpResult index(ExpResult index) throws ExpError {
			if (index.type != ExpResType.NUMBER) {
				throw new ExpError(null, 0, "ArrayList is not being indexed by a number");
			}

			int indexVal = (int)index.value - 1; // Expressions use 1-base arrays

			if (indexVal >= list.size()  || indexVal < 0) {
				return ExpResult.makeNumResult(0, DimensionlessUnit.class); // TODO: Is this how we want to handle this case?
			}
			return list.get(indexVal);
		}

		@Override
		public void assign(ExpResult index, ExpResult value) throws ExpError {
			if (index.type != ExpResType.NUMBER) {
				throw new ExpError(null, 0, "Assignment is not being indexed by a number");
			}
			int indexVal = (int)index.value - 1; // Expressions use 1-base arrays
			if (indexVal < 0) {
				throw new ExpError(null, 0, "Attempting to assign to a negative number: %d", indexVal);
			}
			if (indexVal >= list.size()) {
				// This is a dynamically expanding list, so fill in until we get to the index
				ExpResult filler = ExpResult.makeNumResult(0, DimensionlessUnit.class);
				list.ensureCapacity(indexVal+1);
				for (int i = list.size(); i <= indexVal; ++i) {
					list.add(filler);
				}
			}
			list.set(indexVal, value);
		}

		private static class Iter implements ExpResult.Iterator {

			private int next = 0;
			private final List<?> list;
			public Iter(List<?> l) {
				this.list = l;
			}

			@Override
			public boolean hasNext() {
				return next < list.size();
			}

			@Override
			public ExpResult nextKey() throws ExpError {
				ExpResult ret = ExpResult.makeNumResult(next + 1, DimensionlessUnit.class);
				next++;
				return ret;
			}
		}

		@Override
		public Iterator getIter() {
			return new Iter(list);
		}

		@Override
		public int getSize() {
			return list.size();
		}
		@Override
		public String getOutputString() {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("{ ");
				for (int i = 0; i < list.size(); ++i) {
					ExpResult val = index(ExpResult.makeNumResult(i+1, DimensionlessUnit.class));
					sb.append(val.getOutputString());
					if (i < list.size() -1) {
						sb.append(", ");
					}
				}
				sb.append("}");
				return sb.toString();

			} catch (ExpError err) {
				return String.format("An error occurred: %s", err.getMessage());
			}
		}

	}
}
