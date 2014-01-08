/*
 *  Copyright (C) 2004-2007  The Chemistry Development Kit (CDK) project
 *
 *  Contact: cdk-devel@lists.sourceforge.net
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.cdk.qsar.descriptors.molecular;

import java.util.List;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.annotations.TestMethod;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.qsar.AbstractMolecularDescriptor;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerResult;

public class SmartHBondAcceptorCountDescriptor extends AbstractMolecularDescriptor implements IMolecularDescriptor {

	// only parameter of this descriptor; true if aromaticity has to be checked
	// prior to descriptor calculation, false otherwise
	private static final String[] names = { "nHBAcc" };

	/**
	 * Constructor for the HBondAcceptorCountDescriptor object
	 */
	public SmartHBondAcceptorCountDescriptor() {}

	/**
	 * Gets the specification attribute of the HBondAcceptorCountDescriptor
	 * object.
	 * 
	 * @return The specification value
	 */
	@TestMethod("testGetSpecification")
	public DescriptorSpecification getSpecification() {
		return new DescriptorSpecification(
				"http://www.blueobelisk.org/ontologies/chemoinformatics-algorithms/#hBondacceptors", this.getClass()
						.getName(), "$Id: e2545ba00b514f857260654ef3f0eb9967a199f2 $", "The Chemistry Development Kit");
	}

	/**
	 * Sets the parameters attribute of the HBondAcceptorCountDescriptor object.
	 * 
	 * @param params a boolean true means that aromaticity has to be checked
	 * @exception CDKException Description of the Exception
	 */
	@TestMethod("testSetParameters_arrayObject")
	public void setParameters(Object[] params) throws CDKException {}

	/**
	 * Gets the parameters attribute of the HBondAcceptorCountDescriptor object.
	 * 
	 * @return The parameters value
	 */
	@TestMethod("testGetParameters")
	public Object[] getParameters() {
		return new Object[0];
	}

	@TestMethod(value = "testNamesConsistency")
	public String[] getDescriptorNames() {
		return names;
	}

	/**
	 * Calculates the number of H bond acceptors.
	 * 
	 * @param atomContainer AtomContainer
	 * @return number of H bond acceptors
	 */
	@TestMethod("testCalculate_IAtomContainer")
	public DescriptorValue calculate(IAtomContainer ac) {
		int hBondAcceptors = 0;

		// org.openscience.cdk.interfaces.IAtom[] atoms = ac.getAtoms();
		// labelled for loop to allow for labelled continue statements within
		// the loop
		atomloop: for (IAtom atom : ac.atoms()) {
			// looking for suitable nitrogen atoms
			if (atom.getSymbol().equals("N") && atom.getFormalCharge() <= 0) {

				// excluding nitrogens that are adjacent to an oxygen
				List<IBond> bonds = ac.getConnectedBondsList(atom);
				int nPiBonds = 0;
				for (IBond bond : bonds) {
					if (bond.getConnectedAtom(atom).getSymbol().equals("O"))
						continue atomloop;
					if (IBond.Order.DOUBLE.equals(bond.getOrder()))
						nPiBonds++;
				}

				// if the nitrogen is aromatic and there are no pi bonds then
				// it's
				// lone pair cannot accept any hydrogen bonds
				if (atom.getFlag(CDKConstants.ISAROMATIC) && nPiBonds == 0)
					continue;

				hBondAcceptors++;
			}
			// looking for suitable oxygen atoms
			else if (atom.getSymbol().equals("O") && atom.getFormalCharge() <= 0) {
				// excluding oxygens that are adjacent to a nitrogen or to an
				// aromatic carbon
				List<IAtom> neighbours = ac.getConnectedAtomsList(atom);
				for (IAtom neighbour : neighbours)
					if (neighbour.getSymbol().equals("N")
							|| (neighbour.getSymbol().equals("C") && neighbour.getFlag(CDKConstants.ISAROMATIC)))
						continue atomloop;
				hBondAcceptors++;
			}
		}

		return new DescriptorValue(getSpecification(), getParameterNames(), getParameters(), new IntegerResult(
				hBondAcceptors), getDescriptorNames());
	}

	/**
	 * Returns the specific type of the DescriptorResult object.
	 * <p/>
	 * The return value from this method really indicates what type of result
	 * will be obtained from the
	 * {@link org.openscience.cdk.qsar.DescriptorValue} object. Note that the
	 * same result can be achieved by interrogating the
	 * {@link org.openscience.cdk.qsar.DescriptorValue} object; this method
	 * allows you to do the same thing, without actually calculating the
	 * descriptor.
	 * 
	 * @return an object that implements the
	 *         {@link org.openscience.cdk.qsar.result.IDescriptorResult}
	 *         interface indicating the actual type of values returned by the
	 *         descriptor in the
	 *         {@link org.openscience.cdk.qsar.DescriptorValue} object
	 */
	@TestMethod("testGetDescriptorResultType")
	public IDescriptorResult getDescriptorResultType() {
		return new IntegerResult(1);
	}

	/**
	 * Gets the parameterNames attribute of the HBondAcceptorCountDescriptor
	 * object.
	 * 
	 * @return The parameterNames value
	 */
	@TestMethod("testGetParameterNames")
	public String[] getParameterNames() {
		return new String[0];
	}

	/**
	 * Gets the parameterType attribute of the HBondAcceptorCountDescriptor
	 * object.
	 * 
	 * @param name Description of the Parameter
	 * @return The parameterType value
	 */
	@TestMethod("testGetParameterType_String")
	public Object getParameterType(String name) {
		return false;
	}
}
