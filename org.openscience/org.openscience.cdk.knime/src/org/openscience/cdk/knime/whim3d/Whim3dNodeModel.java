/*
 * Copyright (c) 2012, Stephan Beisken (sbeisken@gmail.com). All rights reserved.
 * 
 * This file is part of the KNIME CDK plugin.
 * 
 * The KNIME CDK plugin is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * The KNIME CDK plugin is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with the plugin. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.openscience.cdk.knime.whim3d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.openscience.cdk.knime.type.CDKValue;

/**
 * This is the model implementation of Whim3d. Holistic descriptors described by Todeschini et al. The descriptors are
 * based on a number of atom weightings. There are 5 different possible weightings implemented.
 * 
 * @author Stephan Beisken, European Bioinformatics Institute
 */
public class Whim3dNodeModel extends NodeModel {

	private Whim3dSettings settings = new Whim3dSettings();
	private int columnIndex;

	/**
	 * Constructor for the node model.
	 */
	protected Whim3dNodeModel() {

		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		DataTableSpec inSpec = inData[0].getDataTableSpec();
		ColumnRearranger rearranger = createColumnRearranger(inSpec);
		BufferedDataTable outTable = exec.createColumnRearrangeTable(inData[0], rearranger, exec);

		return new BufferedDataTable[] { outTable };
	}

	/**
	 * Generates the output table specification and appends the calculated molecular properties to the input table.
	 */
	private ColumnRearranger createColumnRearranger(DataTableSpec spec) throws InvalidSettingsException {

		List<DataColumnSpec> dataColumnSpecs = createOutputTableSpecification();

		final int colIndex = spec.findColumnIndex(settings.getMolColumnName());

		Whim3dGenerator generator = new Whim3dGenerator(colIndex, dataColumnSpecs.toArray(new DataColumnSpec[] {}));
		ColumnRearranger arrange = new ColumnRearranger(spec);
		arrange.append(generator);

		return arrange;
	}

	/**
	 * Creates the table output specification.
	 */
	private List<DataColumnSpec> createOutputTableSpecification() {

		List<DataColumnSpec> dataColumnSpecs = new ArrayList<DataColumnSpec>();

		if (settings.isSchemeUnitWeights())
			createColumnSpec(dataColumnSpecs, Whim3dSchemes.UNITY_WEIGHTS.getTitle(),
					ListCell.getCollectionType(DoubleCell.TYPE));
		if (settings.isSchemeAtomicMasses())
			createColumnSpec(dataColumnSpecs, Whim3dSchemes.ATOMIC_MASSES.getTitle(),
					ListCell.getCollectionType(DoubleCell.TYPE));
		if (settings.isSchemeAtomicPolariz())
			createColumnSpec(dataColumnSpecs, Whim3dSchemes.ATOMIC_POLARIZABILITIES.getTitle(),
					ListCell.getCollectionType(DoubleCell.TYPE));
		if (settings.isSchemeVdWVolumes())
			createColumnSpec(dataColumnSpecs, Whim3dSchemes.VdW_VOLUMES.getTitle(),
					ListCell.getCollectionType(DoubleCell.TYPE));
		if (settings.isSchemeAtomicElectronneg())
			createColumnSpec(dataColumnSpecs, Whim3dSchemes.ATOMIC_ELECTRONEGATIVITIES.getTitle(),
					ListCell.getCollectionType(DoubleCell.TYPE));

		return dataColumnSpecs;
	}

	/**
	 * Creates a single column specification.
	 */
	private void createColumnSpec(List<DataColumnSpec> dataColumnSpecs, String colName, DataType cellType) {

		DataColumnSpec colSpec = new DataColumnSpecCreator(colName, cellType).createSpec();
		dataColumnSpecs.add(colSpec);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		columnIndex = inSpecs[0].findColumnIndex(settings.getMolColumnName());
		if (columnIndex == -1) {
			int i = 0;
			for (DataColumnSpec spec : inSpecs[0]) {
				if (spec.getType().isCompatible(CDKValue.class)) {
					if (columnIndex != -1) {
						setWarningMessage("Column '" + spec.getName() + "' automatically chosen as molecule column");
					}
					columnIndex = i;
				}
				i++;
			}

			if (columnIndex == -1) {
				throw new InvalidSettingsException("Column does not exist");
			}
		}

		if (!inSpecs[0].getColumnSpec(columnIndex).getType().isCompatible(CDKValue.class)) {
			throw new InvalidSettingsException("Column does not contain CDK cells");
		}

		ColumnRearranger arranger = createColumnRearranger(inSpecs[0]);
		return new DataTableSpec[] { arranger.createSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {

		this.settings.saveSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {

		this.settings.loadSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		Whim3dSettings tmpSettings = new Whim3dSettings();
		tmpSettings.loadSettings(settings);

		if ((tmpSettings.getMolColumnName() == null) || (tmpSettings.getMolColumnName().length() == 0)) {
			throw new InvalidSettingsException("No CDK molecule column chosen");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException,
			CanceledExecutionException {

		// nothing to do
	}

}