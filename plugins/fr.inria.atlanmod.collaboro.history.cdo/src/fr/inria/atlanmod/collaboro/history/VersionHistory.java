/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package fr.inria.atlanmod.collaboro.history;

import org.eclipse.emf.cdo.CDOObject;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Version History</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link fr.inria.atlanmod.collaboro.history.VersionHistory#getType <em>Type</em>}</li>
 *   <li>{@link fr.inria.atlanmod.collaboro.history.VersionHistory#getVersions <em>Versions</em>}</li>
 * </ul>
 * </p>
 *
 * @see fr.inria.atlanmod.collaboro.history.HistoryPackage#getVersionHistory()
 * @model
 * @extends CDOObject
 * @generated
 */
public interface VersionHistory extends CDOObject {
	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * The literals are from the enumeration {@link fr.inria.atlanmod.collaboro.history.VersionHistoryType}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Type</em>' attribute isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see fr.inria.atlanmod.collaboro.history.VersionHistoryType
	 * @see #setType(VersionHistoryType)
	 * @see fr.inria.atlanmod.collaboro.history.HistoryPackage#getVersionHistory_Type()
	 * @model required="true"
	 * @generated
	 */
	VersionHistoryType getType();

	/**
	 * Sets the value of the '{@link fr.inria.atlanmod.collaboro.history.VersionHistory#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see fr.inria.atlanmod.collaboro.history.VersionHistoryType
	 * @see #getType()
	 * @generated
	 */
	void setType(VersionHistoryType value);

	/**
	 * Returns the value of the '<em><b>Versions</b></em>' containment reference list.
	 * The list contents are of type {@link fr.inria.atlanmod.collaboro.history.Version}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Versions</em>' containment reference list isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Versions</em>' containment reference list.
	 * @see fr.inria.atlanmod.collaboro.history.HistoryPackage#getVersionHistory_Versions()
	 * @model containment="true"
	 * @generated
	 */
	EList<Version> getVersions();

} // VersionHistory
