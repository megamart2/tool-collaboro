package fr.inria.atlanmod.collaboro.metrics.tools;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import fr.inria.atlanmod.collaboro.metrics.model.AttributeConcept;
import fr.inria.atlanmod.collaboro.metrics.model.ClassConcept;
import fr.inria.atlanmod.collaboro.metrics.model.ReferenceConcept;
import fr.inria.atlanmod.collaboro.metrics.model.Symbol;
import fr.inria.atlanmod.collaboro.metrics.model.VisualRepresentation;
import fr.inria.atlanmod.collaboro.notation.AttributeValue;
import fr.inria.atlanmod.collaboro.notation.Composite;
import fr.inria.atlanmod.collaboro.notation.Definition;
import fr.inria.atlanmod.collaboro.notation.GraphicalElement;
import fr.inria.atlanmod.collaboro.notation.NotationElement;
import fr.inria.atlanmod.collaboro.notation.ReferenceValue;
import fr.inria.atlanmod.collaboro.notation.SyntaxOf;

public class ModelElementExtractor {
	
	private EPackage abstractSyntaxModel;
	private Definition concreteSyntaxModel;
	
	private List<ClassConcept> classConcepts;
	private List<AttributeConcept> attributeConcepts;
	private List<ReferenceConcept> referenceConcepts;
	
	private List<Symbol> symbols;
	
	private ModelMapping modelMapping;
	
	private ConcreteSyntaxElementExtractor concreteSyntaxElementExtractor;
	
	public ModelElementExtractor() {
		this.classConcepts = new ArrayList<ClassConcept>();
		this.attributeConcepts = new ArrayList<AttributeConcept>();
		this.referenceConcepts = new ArrayList<ReferenceConcept>();
		this.symbols = new ArrayList<Symbol>();
	}
	
	public ModelElementExtractor(EPackage abstractSyntaxModel, Definition concreteSyntaxModel) {
		this.abstractSyntaxModel = abstractSyntaxModel;
		this.concreteSyntaxModel = concreteSyntaxModel;
		this.classConcepts = new ArrayList<ClassConcept>();
		this.attributeConcepts = new ArrayList<AttributeConcept>();
		this.referenceConcepts = new ArrayList<ReferenceConcept>();
		this.symbols = new ArrayList<Symbol>();
		
		
		this.concreteSyntaxElementExtractor = new ConcreteSyntaxElementExtractor(concreteSyntaxModel);
		
		initialize();
	}
	
	private void initialize() {
		System.out.println(" ------------ Initializing model element extractor ------------");
		System.out.println(" ******* Concept extraction *******");
		System.out.println("");
		// Abstract Syntax discovery
		discoverAbstractClasses();
		discoverAbstractAttribute();
		discoverAbstractReference();
		
		System.out.println("");
		System.out.println(" ******* Symbol extraction *******");
		System.out.println("");
		// Concrete Syntax discovery
		//discoverConcreteConcepts();
		//concreteConcepts();
		this.symbols = this.concreteSyntaxElementExtractor.discoverConcreteSyntax();
		//System.out.println(concreteSymbols);
		
		this.modelMapping = new ModelMapping(classConcepts,attributeConcepts,referenceConcepts, symbols);
		
		printResult();
		System.out.println("---------- ");
		printConcreteSymbols();
		
	}
	
	public ModelMapping getModelMapping() {
		return modelMapping;
	}
	
	/* 
	 *  ================================================================
	 * 			Abstract Syntax Concept Extraction Methods
	 *  ================================================================
	*/
	private void discoverAbstractClasses() {
		System.out.println("* Discover Abstract Class");
		List<EObject> abstractSyntaxContents = abstractSyntaxModel.eContents();
		for(EObject abstractSyntaxElement : abstractSyntaxContents) {
			if(abstractSyntaxElement instanceof EClass) {
				EClass eClassElement = (EClass) abstractSyntaxElement;
				ClassConcept classConcept = new ClassConcept(eClassElement.getName(), eClassElement.getName(), eClassElement);
				classConcepts.add(classConcept);
				System.out.println("\t Found Class : " + classConcept.getName());
			}
		}
		
		// extracting heritage information
		resolveClassHeritage();
	}
	
	private void resolveClassHeritage() {
		System.out.println("* Resolve class heritage");
		for(ClassConcept classConcept : classConcepts) {
			EClass eClass = (EClass) classConcept.getAbstractModelElement();
			List<EClass> eClassSuperTypes = eClass.getESuperTypes();
			for(EClass eClassSuperType : eClassSuperTypes) {
				ClassConcept classConceptSuperType = getClassConceptById(eClassSuperType.getName());
				if(classConceptSuperType != null) {
					classConceptSuperType.addSubType(classConcept);
					System.out.println("\t Class " + classConceptSuperType.getName() + " subType : " + classConcept.getName());
					classConcept.addSuperType(classConceptSuperType);
					System.out.println("\t Class " + classConcept.getName() + " superType : " + classConceptSuperType.getName());
				}
			}
		}
	}
	
	private void discoverAbstractAttribute() {
		System.out.println("* Discover Abstract Attribute");
		for(ClassConcept classConcept : classConcepts) {
			EClass eClass = (EClass) classConcept.getAbstractModelElement();
			List<EAttribute> eClassAttributes = eClass.getEAllAttributes();
			for(EAttribute eClassAttribute : eClassAttributes) {
				String attributeName = eClass.getName() + "." + eClassAttribute.getName();
				AttributeConcept attributeConcept = new AttributeConcept(eClassAttribute.getName(), attributeName, eClassAttribute);
				attributeConcept.setClassConcept(classConcept);
				System.out.println("\t Found Attribute : " + attributeConcept.getName() + " in Class : " + classConcept.getName());

				EClass eContainingClass = eClassAttribute.getEContainingClass();
				if(!eContainingClass.equals(eClass)) {
					ClassConcept containingClassConcept = getClassConceptById(eContainingClass.getName());
					if(containingClassConcept != null) {
						attributeConcept.setContainingSuperClass(containingClassConcept);
						System.out.println("\t\t From superClass : " + containingClassConcept.getName());
					}
				}
				this.attributeConcepts.add(attributeConcept);
				classConcept.addAttribute(attributeConcept);
			}
		}
		
		// extracting heritage information
		for(AttributeConcept attributeConcept : attributeConcepts) {
			ClassConcept attributeSuperClass = attributeConcept.getContainingSuperClass();
			if(attributeSuperClass == null) {
				// not heritated attribute
				resolveAttributeHeritage(attributeConcept);
			}
		}
	}
	
	private void resolveAttributeHeritage(AttributeConcept attributeConcept) {
		System.out.println("* Resolve attribute heritage : " + attributeConcept.getName());
		EObject modelAttribute = attributeConcept.getAbstractModelElement();
		ClassConcept containingClass = attributeConcept.getClassConcept();
		List<ClassConcept> containingClassSubClasses = containingClass.getSubTypes();
		for(ClassConcept subClassConcept : containingClassSubClasses) {
			AttributeConcept subAttributeConcept = getAttributeConceptByEObjectFromClassConcept(subClassConcept, modelAttribute);
			if(subAttributeConcept != null) {
				attributeConcept.addSubAttribute(subAttributeConcept);
				subAttributeConcept.addSuperAttribute(attributeConcept);
				System.out.println("\t " + attributeConcept.getName() + " is superType of " + subAttributeConcept.getName());
				resolveAttributeHeritage(subAttributeConcept);
			}
		}
		
	}
	
	private AttributeConcept getAttributeConceptByEObjectFromClassConcept(ClassConcept classConcept, EObject modelObject) {
		List<AttributeConcept> classAttributes = classConcept.getAttributes();
		for(AttributeConcept classAttribute : classAttributes) {
			if(classAttribute.getAbstractModelElement().equals(modelObject)) {
				return classAttribute;
			}
		}
		return null;
	}
	
	private void discoverAbstractReference() {
		System.out.println("* Discover Abstract Reference");
		for(ClassConcept classConcept : classConcepts) {
			EClass eClass = (EClass) classConcept.getAbstractModelElement();
			List<EReference> eClassReferences = eClass.getEAllReferences();
			for(EReference eClassReference : eClassReferences) {
				String referenceName = eClass.getName() + "." + eClassReference.getName();
				ReferenceConcept referenceConcept = new ReferenceConcept(eClassReference.getName(), referenceName, eClassReference);
				
				referenceConcept.setContainingClass(classConcept);
				//From
				EClass eClassReferenceFrom = eClassReference.getEContainingClass();
				ClassConcept referenceClassConceptFrom = getClassConceptById(eClassReferenceFrom.getName());
				referenceConcept.setSuperClassConceptFrom(referenceClassConceptFrom);
				//To
				EClass eClassReferenceTo = eClassReference.getEReferenceType();
				ClassConcept referenceClassConceptTo = getClassConceptById(eClassReferenceTo.getName());
				referenceConcept.setClassConceptTo(referenceClassConceptTo);
				
				referenceConcept.setSuperClassConceptTo(referenceClassConceptTo);
				
				referenceConcepts.add(referenceConcept);
				classConcept.addReference(referenceConcept);
				System.out.println("\t Found Reference : " + referenceConcept.getName() + " in Class : " + classConcept.getName());
			}
		}
		
		//Opposite Discovery
		List<ReferenceConcept> oppositeReferenceConcepts = resolveReferenceConceptOpposite();
		referenceConcepts.addAll(oppositeReferenceConcepts);
		
		//extracting heritage information
		resolveReferenceHeritage();
		
		
	}
	
	private List<ReferenceConcept> resolveReferenceConceptOpposite() {
		System.out.println("* Resolve Reference Opposite");
		List<ReferenceConcept> oppositeReferenceConcepts = new ArrayList<ReferenceConcept>();
		for(ReferenceConcept referenceConcept : referenceConcepts) {
			EReference eReference = (EReference) referenceConcept.getAbstractModelElement();
			EReference eOppositeReference = eReference.getEOpposite();
			if(eOppositeReference != null) {
				System.out.println("\t Found opposite for " + referenceConcept.getName());
				//To
				ClassConcept referenceContainingClass = referenceConcept.getContainingClass();
				EClass eOppositeReferenceToEClass = eOppositeReference.getEReferenceType();
				ClassConcept oppositeReferenceToClassConcept = getClassConceptById(eOppositeReferenceToEClass.getName());
				//From
				EClass eOppositeReferenceContainingClass = eOppositeReference.getEContainingClass();
				ClassConcept oppositeReferenceContainingClass = getClassConceptById(eOppositeReferenceContainingClass.getName());
				
				ReferenceConcept oppositeReferenceConcept = getExistingReferenceConcept(oppositeReferenceContainingClass,referenceContainingClass,eOppositeReference);
				
				if(oppositeReferenceConcept == null) {
					
					String oppositeReferenceName = eOppositeReferenceContainingClass.getName() + "." + eOppositeReference.getName();
					oppositeReferenceConcept = new ReferenceConcept(eOppositeReference.getName(), oppositeReferenceName, eOppositeReference);
					oppositeReferenceConcept.setContainingClass(oppositeReferenceContainingClass);
					oppositeReferenceConcept.setSuperClassConceptFrom(oppositeReferenceContainingClass);
					oppositeReferenceConcept.setSuperClassConceptTo(oppositeReferenceToClassConcept);
					oppositeReferenceConcept.setClassConceptTo(referenceContainingClass);
					oppositeReferenceConcept.setReferenceOpposite(referenceConcept);
					referenceConcept.setReferenceOpposite(oppositeReferenceConcept);
					oppositeReferenceConcepts.add(oppositeReferenceConcept);
					oppositeReferenceContainingClass.addReference(oppositeReferenceConcept);
					System.out.println("\t\t non existing opposite : " + oppositeReferenceConcept.getName());
				} else {
					referenceConcept.setReferenceOpposite(oppositeReferenceConcept);
					System.out.println("\t\t existing opposite " + oppositeReferenceConcept.getName());
				}
			}
		}
		return oppositeReferenceConcepts;
	}
	
	private void resolveReferenceHeritage() {
		System.out.println("* Resolve reference heritage");
		for(ReferenceConcept referenceConcept : referenceConcepts) {
			ClassConcept referenceContainingClass = referenceConcept.getContainingClass();
			ClassConcept referenceSuperContainingClass = referenceConcept.getSuperClassConceptFrom();
			ClassConcept referenceToClass = referenceConcept.getClassConceptTo();
			ClassConcept referenceSuperToClass = referenceConcept.getSuperClassConceptTo();
			if(referenceSuperContainingClass.equals(referenceContainingClass)) {
				if(referenceSuperToClass.equals(referenceToClass)) {
					// not heritated Reference
					resolveRH(referenceConcept);
					resolveRH2(referenceConcept);
				}	
			}
		}
	}

	
	private void resolveRH(ReferenceConcept referenceConcept) {
		EReference modelReference = (EReference) referenceConcept.getAbstractModelElement();
		ClassConcept containingClass = referenceConcept.getContainingClass();
		ClassConcept referenceToSuperClass = referenceConcept.getSuperClassConceptTo();
		List<ClassConcept> containingClassSubClasses = containingClass.getSubTypes();
		for(ClassConcept subClassConcept : containingClassSubClasses) {
			ReferenceConcept subReferenceConcept = getExistingReferenceConcept(subClassConcept,referenceToSuperClass,modelReference);
			if(subReferenceConcept != null) {
				referenceConcept.addSubReference(subReferenceConcept);
				subReferenceConcept.addSuperReference(referenceConcept);
				resolveRH(subReferenceConcept);
			}
		}
	}
	
	private void resolveRH2(ReferenceConcept referenceConcept) {
		EReference modelReference = (EReference) referenceConcept.getAbstractModelElement();
		ClassConcept referenceFromClass = referenceConcept.getSuperClassConceptFrom();
		ClassConcept referenceToClass = referenceConcept.getClassConceptTo();
		List<ClassConcept> referenceToClassSubClasses = referenceToClass.getSubTypes();
		for(ClassConcept referenceToClassSubClass : referenceToClassSubClasses) {
			ReferenceConcept subReferenceConcept = getExistingReferenceConcept(referenceFromClass,referenceToClassSubClass,modelReference);
			if(subReferenceConcept != null) {
				subReferenceConcept.addSuperReference(referenceConcept);
				referenceConcept.addSubReference(subReferenceConcept);
				resolveRH2(subReferenceConcept);
			}
		}
	}
	
	private ReferenceConcept getExistingReferenceConcept(ClassConcept classConceptFrom,ClassConcept classConceptTo, EReference eReference) {
		List<ReferenceConcept> classConceptReferences = classConceptFrom.getReferences();
		for(ReferenceConcept classConceptReference : classConceptReferences) {
			ClassConcept classConceptReferenceTo = classConceptReference.getClassConceptTo();
			if(classConceptReferenceTo.equals(classConceptTo)) {
				EReference classConceptReferenceEReference = (EReference) classConceptReference.getAbstractModelElement();
				if(classConceptReferenceEReference.equals(eReference)) {
					return classConceptReference;
				}
			}
		}
		return null;
	}
	
	private ClassConcept getClassConceptById(String id) {
		for(ClassConcept classConcept : classConcepts) {
			if(classConcept.getAbstractModelId().equals(id)) {
				return classConcept;
			}
		}
		return null;
	}	
	
	/* 
	 *  ================================================================
	 * 			Concrete Syntax Symbol Extraction Methods
	 *  ================================================================
	*/
	
//	private void resolvePrimaryComposite(Composite composite) {
//		System.out.println("In resolvePrimaryComposite : " + composite.getId());
//		String compositeId = composite.getId();
//		String[] splitCompositeId = compositeId.split("\\.");
//		String compositeAttachedConceptName = "";
//		if(splitCompositeId.length == 3) {
//			String referenceClassName = splitCompositeId[0];
//			String referenceReferenceName = splitCompositeId[1];
//			String referenceAttributeName = splitCompositeId[2];
//			compositeAttachedConceptName = referenceClassName + "." + referenceReferenceName;
//			ReferenceConcept correspondingReferenceConcept = getReferenceConcept(compositeAttachedConceptName);
////			if(correspondingReferenceConcept != null) {
//				ReferenceSymbol referenceSymbol = new ReferenceSymbol(compositeId, referenceClassName, referenceReferenceName, referenceAttributeName, composite);
//				referenceSymbols.add(referenceSymbol);
//				System.out.println("\t Found Reference : " + compositeAttachedConceptName);
////			}
//			
//		} else if(splitCompositeId.length == 2) {
//			String attributeClassName = splitCompositeId[0];
//			String attributeAttributeName = splitCompositeId[1];
//			compositeAttachedConceptName = attributeClassName + "." + attributeAttributeName;
//			AttributeConcept correspondingAttributeConcept = getAttributeConcept(compositeAttachedConceptName);
////			if(correspondingAttributeConcept != null) {
//				AttributeSymbol attributeSymbol = new AttributeSymbol(compositeId,attributeClassName, attributeAttributeName,composite);
//				attributeSymbols.add(attributeSymbol);
//				System.out.println("\t Found Attribute : " + compositeAttachedConceptName);
////			}
//			
//		} else if(splitCompositeId.length == 1) {
//			compositeAttachedConceptName = compositeId;
//			ClassConcept correspondingClassConcept = getClassConcept(compositeAttachedConceptName);
////			if(correspondingClassConcept != null) {
//				ClassSymbol classSymbol = new ClassSymbol(compositeId, compositeAttachedConceptName, composite);
//				classSymbols.add(classSymbol);
//				System.out.println("\t Found Class : " + compositeAttachedConceptName);
////			}
//		} else {
//			
//		}
//		
//	}
//	
//	private void resolveSyntaxOf(SyntaxOf syntaxOf, Composite containingComposite) {
//		if(containingComposite != null) {
//			String containingCompositeName = containingComposite.getId();
//			ClassSymbol containingCompositeClassSymbol = getClassSymbol(containingCompositeName);
//			if(containingCompositeClassSymbol != null) {
//				
//			}
//		} else {
//			
//		}
//		System.out.println("In resolveSyntaxOf : " + syntaxOf.getId() + " , " + containingComposite.getId());
//		EReference reference = syntaxOf.getReference();
//		if(reference != null) {
//			String syntaxOfName = syntaxOf.getId();
//			String[] splitSyntaxOfName = syntaxOfName.split("\\.");
//			String syntaxOfAttachedConceptName = "";
//			if(splitSyntaxOfName.length == 3) {
//				String referenceClassName = splitSyntaxOfName[0];
//				String referenceReferenceName = splitSyntaxOfName[1];
//				String referenceAttributeName = splitSyntaxOfName[2];
//				syntaxOfAttachedConceptName = referenceClassName + "." + referenceReferenceName;
//				ReferenceSymbol existingReferenceSymbol = getReferenceSymbol(syntaxOfName);
//				if(existingReferenceSymbol != null) {
//					//symbol already exists
//				}
//			}
//		} else {
//			
//		}
//	}
//	
//	private void resolveReferenceValue(ReferenceValue referenceValue, Composite containingComposite) {
//		
//		if(containingComposite != null) {
//			System.out.println("In resolveReferenceValue : " + referenceValue.getId() + " , " + containingComposite.getId());
//			String containingCompositeName = containingComposite.getId();
//			ClassSymbol containingCompositeClassSymbol = getClassSymbol(containingCompositeName);
//			if(containingCompositeClassSymbol != null) {
//				//Reference in Class
//				// need to be registered
//				EReference referenceEReference = referenceValue.getReference();
//				EAttribute referenceEAttribute = referenceValue.getAttribute();
//				if((referenceEReference == null) && (referenceEAttribute == null)) {
//					String referenceName = referenceValue.getId();
//					String[] splitReferenceName = referenceName.split("\\.");
//					if(splitReferenceName.length == 3) {
//						ReferenceSymbol referenceSymbol = new ReferenceSymbol(referenceName, splitReferenceName[0], splitReferenceName[1], splitReferenceName[2], referenceValue);
//						referenceSymbols.add(referenceSymbol);
//						System.out.println("\t Found Reference in Class " + containingCompositeName + " : " + referenceName);
//					}
//				}
//			} else {
//				
//			}
//			ReferenceSymbol containingCompositeReferenceSymbol = getReferenceSymbol(containingCompositeName);
//			if(containingCompositeReferenceSymbol != null) {
//				//ReferenceValue in reference
//				// containing composite is already the reference symbol
//			}
//			
//		} else {
//			System.out.println("In resolveReferenceValue : " + referenceValue.getId() + " , null" );
//			EReference referenceEReference = referenceValue.getReference();
//			EAttribute referenceEAttribute = referenceValue.getAttribute();
//			if((referenceEReference == null) && (referenceEAttribute == null)) {
//				String referenceName = referenceValue.getId();
//				String[] splitReferenceName = referenceName.split("\\.");
//				if(splitReferenceName.length == 3) {
//					ReferenceSymbol referenceSymbol = new ReferenceSymbol(referenceName, splitReferenceName[0], splitReferenceName[1], splitReferenceName[2], referenceValue);
//					referenceSymbols.add(referenceSymbol);
//					System.out.println("\t Found Reference : " + referenceName);
//				}
//			}
//		}
//	}
//	
//	
//	private void resolveAttributeValue(AttributeValue attributeValue, Composite containingComposite) {
//		
//		if(containingComposite != null) {
//			System.out.println("In resolveAttributeValue : " + attributeValue.getId() + " , " + containingComposite.getId());
//			String containingCompositeName = containingComposite.getId();
//			ClassSymbol containingCompositeClassSymbol = getClassSymbol(containingCompositeName);
//			if(containingCompositeClassSymbol != null) {
//				//Attribute in Class
//				// need to be registered
//				EAttribute attributeEAttribute = attributeValue.getAttribute();
//				if(attributeEAttribute == null) {
//					String attributeName = attributeValue.getId();
//					String[] splitAttributeName = attributeName.split("\\.");
//					if(splitAttributeName.length == 2) {
//						AttributeSymbol attributeSymbol = new AttributeSymbol(attributeName, splitAttributeName[0], splitAttributeName[1], attributeValue);
//						attributeSymbols.add(attributeSymbol);
//						System.out.println("\t Found Attribute in Class " + containingCompositeName + " : " + attributeName);
//					}
//				}
//			} else {
//				
//			}
//			AttributeSymbol containingCompositeAttributeSymbol = getAttributeSymbol(containingCompositeName);
//			if(containingCompositeAttributeSymbol != null) {
//				//AttributeValue in attribute symbol
//				// containing composite is already the attribute symbol
//			}
//			
//		} else {
//			System.out.println("In resolveAttributeValue : " + attributeValue.getId() + " , null");
//			EAttribute attributeEAttribute = attributeValue.getAttribute();
//			if(attributeEAttribute == null) {
//				String attributeName = attributeValue.getId();
//				String[] splitAttributeName = attributeName.split("\\.");
//				if(splitAttributeName.length == 2) {
//					AttributeSymbol attributeSymbol = new AttributeSymbol(attributeName, splitAttributeName[0], splitAttributeName[1], attributeValue);
//					attributeSymbols.add(attributeSymbol);
//					System.out.println("\t Found Attribute : " + attributeName);
//				}
//			}
//		}
//	}
//	
//	private void resolveGraphicalElement(GraphicalElement graphicalElement, Composite containingComposite) {
//		
//	}
//	
//	private void resolveSubComposite(Composite subComposite, Composite parentComposite) {
//		
//	}
//	
//
//	public void discoverConcreteConcepts() {
//		System.out.println("* Discover Conrete Concepts");
//		List<NotationElement> concreteSyntaxElements = concreteSyntaxModel.getElements();
//		for(NotationElement concreteSyntaxElement : concreteSyntaxElements) {
//			if(concreteSyntaxElement instanceof Composite) {
//				Composite compositeElement = (Composite) concreteSyntaxElement;
//				resolvePrimaryComposite(compositeElement);
//				
//				// check Elements in composite
//				TreeIterator<EObject> compositeContents = compositeElement.eAllContents();
//				while(compositeContents.hasNext()) {
//					EObject compositeContent = compositeContents.next();
//					if( compositeContent instanceof Composite) {
//						Composite subComposite = (Composite) compositeContent;
//						resolveSubComposite(subComposite, compositeElement);
//					} else if(compositeContent instanceof AttributeValue) {
//						AttributeValue attributeElement = (AttributeValue) compositeContent;
//						resolveAttributeValue(attributeElement, compositeElement);
//					} else if(compositeContent instanceof ReferenceValue) {
//						ReferenceValue referenceElement = (ReferenceValue) compositeContent;
//						resolveReferenceValue(referenceElement, compositeElement);
//					} else if(compositeContent instanceof SyntaxOf) {
//						SyntaxOf syntaxOfElement = (SyntaxOf) compositeContent;
//						resolveSyntaxOf(syntaxOfElement, compositeElement);
//					} else if(compositeContent instanceof GraphicalElement) {
//						GraphicalElement graphicalElement = (GraphicalElement) compositeContent;
//						resolveGraphicalElement(graphicalElement,compositeElement);
//					}
//				}
//				
//			} else if(concreteSyntaxElement instanceof AttributeValue) {
//				AttributeValue attributeElement = (AttributeValue) concreteSyntaxElement;
//				resolveAttributeValue(attributeElement, null);
//			} else if(concreteSyntaxElement instanceof ReferenceValue) {
//				ReferenceValue referenceElement = (ReferenceValue) concreteSyntaxElement;
//				resolveReferenceValue(referenceElement, null);
//			} else if(concreteSyntaxElement instanceof SyntaxOf) {
//				SyntaxOf syntaxOfElement = (SyntaxOf) concreteSyntaxElement;
//				resolveSyntaxOf(syntaxOfElement, null);
//			}
//		}
//	}
	
//	public void discoverConcreteConcepts() {
//		
//		System.out.println("Discovery of Concrete Symbols : ");
//		List<NotationElement> concreteSyntaxElements = concreteSyntaxModel.getElements();
//		
//		for(NotationElement concreteSyntaxElement : concreteSyntaxElements) {
//			if(concreteSyntaxElement instanceof Composite) {
//				Composite compositeElement = (Composite) concreteSyntaxElement;
//				String compositeElementId = compositeElement.getId();
//				boolean isClassComposite = false;
//				boolean isAttributeComposite = false;
//				boolean isReferenceComposite = false;
//				// Check the type of the composite (Class, Attribute, Reference)
//				String[] splitCompositeElementId = compositeElementId.split("\\.");
//				if(splitCompositeElementId.length == 3) {
//					// Reference symbol
//					String referenceClassName = splitCompositeElementId[0];
//					String referenceReferenceName = splitCompositeElementId[1];
//					String referenceAttributeName = splitCompositeElementId[2];
//					isReferenceComposite = true;
//					/*Symbol referenceSymbol = new ReferenceSymbol(compositeElementId, referenceClassName, referenceReferenceName, referenceAttributeName, compositeElement);
//					concreteSymbols.add(referenceSymbol);
//					
//					System.out.println("Found Reference : " + compositeElementId + " -> " + compositeElement);*/
//				} else if (splitCompositeElementId.length == 2){
//					// Attribute symbol
//					String attributeClassName = splitCompositeElementId[0];
//					String attributeAttributeName = splitCompositeElementId[1];
//					isAttributeComposite = true;
//					
//					/*Symbol referenceSymbol = new AttributeSymbol(compositeElementId, attributeClassName, attributeAttributeName, compositeElement);
//					concreteSymbols.add(referenceSymbol);
//					System.out.println("Found Attribute : " + compositeElementId + " -> " + compositeElement);*/
//				} else if (splitCompositeElementId.length == 1) {
//					//Class symbol
//					isClassComposite = true;
//					String className = compositeElementId;
//					
//					ClassSymbol classSymbol = new ClassSymbol(compositeElementId, compositeElementId, compositeElement);
//					classSymbols.add(classSymbol);
//					System.out.println("Found Class : " + compositeElementId + " -> " + compositeElement);
//				}
//				
//				// Discovering component contents
//				TreeIterator<EObject> compositeContents = compositeElement.eAllContents();
//				while(compositeContents.hasNext()) {
//					EObject compositeContent = compositeContents.next();
//					if(compositeContent instanceof AttributeValue) {
//						AttributeValue compositeAttributeValue = (AttributeValue) compositeContent;
//						String attributeValueId = compositeAttributeValue.getId();
//						EAttribute attributeValueEAttribute = compositeAttributeValue.getAttribute();
//						String attributeValueClassName = "";
//						String attributeValueAttributeName = "";
//						if(attributeValueEAttribute != null) {
//							//TODO Check 
//							attributeValueClassName = attributeValueEAttribute.getEContainingClass().getName();
//							attributeValueAttributeName = attributeValueEAttribute.getName();
//						} else {
//							String[] splitAttributeValueId = attributeValueId.split("\\.");
//							attributeValueClassName = splitAttributeValueId[0];
//							attributeValueAttributeName = splitAttributeValueId[1];
//						}
//						AttributeSymbol attributeSymbol = new AttributeSymbol(attributeValueId, attributeValueClassName, attributeValueAttributeName, compositeAttributeValue);
//						attributeSymbols.add(attributeSymbol);
//						System.out.println("Found Attribute : " + attributeValueId + " -> " + compositeAttributeValue);
//						
//						
//					} else if (compositeContent instanceof ReferenceValue) {
//						ReferenceValue compositeReferenceValue = (ReferenceValue) compositeContent;
//						String referenceValueId = compositeReferenceValue.getId();
//						EAttribute referenceValueEAttribute = compositeReferenceValue.getAttribute();
//						EReference referenceValueEReference = compositeReferenceValue.getReference();
//						String referenceValueClassName = "";
//						String referenceValueReferenceName = "";
//						String referenceValueAttributeName = "";
//						if(referenceValueEReference != null) {
//							referenceValueClassName = referenceValueEReference.getEContainingClass().getName();
//							referenceValueReferenceName = referenceValueEReference.getName();
//							if(referenceValueEAttribute != null) {
//								referenceValueAttributeName = referenceValueEAttribute.getName();
//							}
//							
//						} else {
//							String[] splitReferenceValueId = referenceValueId.split("\\.");
//							if(splitReferenceValueId.length == 3) {
//								referenceValueClassName = splitReferenceValueId[0];
//								referenceValueReferenceName = splitReferenceValueId[1];
//								referenceValueAttributeName = splitReferenceValueId[2];
//							}
//						}
//						
//						ReferenceSymbol referenceSymbol = new ReferenceSymbol(referenceValueId, referenceValueClassName, referenceValueReferenceName, referenceValueAttributeName, compositeReferenceValue);
//						referenceSymbols.add(referenceSymbol);
//						System.out.println("Found Reference1 : " + referenceValueId + " -> " + compositeReferenceValue);
//						
//						
//					} else if (compositeContent instanceof SyntaxOf) {
//						SyntaxOf compositeSyntaxOf = (SyntaxOf) compositeContent;
//						String syntaxOfId = compositeSyntaxOf.getId();
//						EReference syntaxOfEReference = compositeSyntaxOf.getReference();
//						if(syntaxOfEReference != null) {
//							
//						} else {
//							String[] splitSyntaxOfId = syntaxOfId.split("\\.");
//							if(splitSyntaxOfId.length == 3) {
//								//Reference syntax
//								String syntaxOfClassName = splitSyntaxOfId[0];
//								if(isClassComposite) {
//									syntaxOfClassName = compositeElementId;
//								}
//								String syntaxOfReferenceName = splitSyntaxOfId[1];
//								String syntaxOfAttributeName = splitSyntaxOfId[2];
//								String syntaxOfName = syntaxOfClassName + "." + syntaxOfReferenceName + "." + syntaxOfAttributeName;
//								ReferenceSymbol referenceSymbol = new ReferenceSymbol(syntaxOfName, syntaxOfClassName, syntaxOfReferenceName, syntaxOfAttributeName, compositeSyntaxOf);
//								referenceSymbols.add(referenceSymbol);
//								System.out.println("Found Reference : " + syntaxOfName + " -> " + compositeSyntaxOf);
//								
//							} else if(splitSyntaxOfId.length == 2) {
//								// Attribute syntax
//								String syntaxOfClassName = splitSyntaxOfId[0];
//								String syntaxOfAttributeName = splitSyntaxOfId[1];
//								
//								if(isClassComposite) {
//									syntaxOfClassName = compositeElementId;
//								}
//								String syntaxOfName = syntaxOfClassName + "." + syntaxOfAttributeName;
//								AttributeSymbol attributeSymbol = new AttributeSymbol(syntaxOfName, syntaxOfClassName, syntaxOfAttributeName, compositeSyntaxOf);
//								attributeSymbols.add(attributeSymbol);
//								System.out.println("Found Attribute : " + syntaxOfName + " -> " + compositeSyntaxOf);
//								
//							} else if(splitSyntaxOfId.length == 1) {
//								// Class syntax
//							}
//						}
//						
//					}
//				}
//			}
//		}
//	}

	
/*	private void resolveComposite(Composite composite) {
		String compositeElementId = composite.getId();
		boolean isClassComposite = false;
		boolean isAttributeComposite = false;
		boolean isReferenceComposite = false;
		// Check the type of the composite (Class, Attribute, Reference)
		String[] splitCompositeElementId = compositeElementId.split("\\.");
		if(splitCompositeElementId.length == 3) {
			// Reference symbol
			String referenceClassName = splitCompositeElementId[0];
			String referenceReferenceName = splitCompositeElementId[1];
			String referenceAttributeName = splitCompositeElementId[2];
			isReferenceComposite = true;
			Symbol referenceSymbol = new ReferenceSymbol(compositeElementId, referenceClassName, referenceReferenceName, referenceAttributeName, composite);
			concreteSymbols.add(referenceSymbol);
			
			System.out.println("Found Reference : " + compositeElementId + " -> " + composite);
		} else if (splitCompositeElementId.length == 2){
			// Attribute symbol
			String attributeClassName = splitCompositeElementId[0];
			String attributeAttributeName = splitCompositeElementId[1];
			isAttributeComposite = true;
			
			Symbol referenceSymbol = new AttributeSymbol(compositeElementId, attributeClassName, attributeAttributeName, composite);
			concreteSymbols.add(referenceSymbol);
			System.out.println("Found Attribute : " + compositeElementId + " -> " + composite);
		} else if (splitCompositeElementId.length == 1) {
			//Class symbol
			isClassComposite = true;
			String className = compositeElementId;
			Symbol classSymbol = new ClassSymbol(compositeElementId, compositeElementId, composite);
			concreteSymbols.add(classSymbol);
			System.out.println("Found Class : " + compositeElementId + " -> " + composite);
		}
	}*/
	
	private ClassConcept getClassConcept(String symbolName) {
		for(ClassConcept classConcept : classConcepts) {
			if(classConcept.getName().equals(symbolName)) {
				return classConcept;
			}
		}
		return null;
	}
	
	private AttributeConcept getAttributeConcept(String symbolName) {
		for(AttributeConcept attributeConcept : attributeConcepts) {
			if(attributeConcept.getName().equals(symbolName)) {
				return attributeConcept;
			}
		}
		return null;
	}
	
	private ReferenceConcept getReferenceConcept(String symbolName) {
		for(ReferenceConcept referenceConcept : referenceConcepts) {
			if(referenceConcept.getName().equals(symbolName)) {
				return referenceConcept;
			}
		}
		return null;
	}
	
//	private ClassSymbol getClassSymbol(String symbolName) {
//		for(ClassSymbol classSymbol : classSymbols) {
//			if(classSymbol.getName().equals(symbolName)) {
//				return classSymbol;
//			}
//		}
//		return null;
//	}
//	
//	private AttributeSymbol getAttributeSymbol(String symbolName) {
//		for(AttributeSymbol attributeSymbol : attributeSymbols) {
//			if(attributeSymbol.getName().equals(symbolName)) {
//				return attributeSymbol;
//			}
//		}
//		return null;
//	}
//	
//	private ReferenceSymbol getReferenceSymbol(String symbolName) {
//		for(ReferenceSymbol referenceSymbol : referenceSymbols) {
//			if(referenceSymbol.getName().equals(symbolName)) {
//				return referenceSymbol;
//			}
//		}
//		return null;
//	}
	
	//TODO debug
	public void printResult() {
		for(ClassConcept classConcept : classConcepts) {
			System.out.println("Class concept : " + classConcept.getAbstractModelId() +  " , " + classConcept.getName());
			System.out.println("\tsubTypes : " + classConcept.getSubTypes());
			System.out.println("\tsuperTypes : " + classConcept.getSuperType());
			System.out.println("\tattributes : " + classConcept.getAttributes());
			System.out.println("\treferences : " + classConcept.getReferences());
		}
		for(AttributeConcept attribute : attributeConcepts) {
			System.out.println("Attribute Concept : " + attribute.getAbstractModelId() + " , " + attribute.getName());
			System.out.println("\tcontainingClass : " + attribute.getClassConcept());
			System.out.println("\tsuper containingClass : " + attribute.getContainingSuperClass());
			System.out.println("\tsubAttributes : " + attribute.getSubAttributes());
			System.out.println("\tsuperAttributes : " + attribute.getSuperAttributes());
		}
		for(ReferenceConcept reference : referenceConcepts) {
			System.out.println("Reference Concept : " + reference.getAbstractModelId() + " , " + reference.getName());
			System.out.println("\tcontaining class : " + reference.getContainingClass());
			System.out.println("\tSuper from class : " + reference.getSuperClassConceptFrom());
			System.out.println("\tSuper to class : " + reference.getSuperClassConceptTo());
			System.out.println("\tto class : " + reference.getClassConceptTo());
			if(reference.getReferenceOpposite() != null) {
				System.out.println("\tOpposite : " + reference.getReferenceOpposite() + "." + reference.getReferenceOpposite().getClassConceptTo());
			} else {
				System.out.println("\tOpposite : " + reference.getReferenceOpposite());
			}
			
			System.out.println("\tsubReferences : " + reference.getSubReferences());
			System.out.println("\tsuperReferences : " + reference.getSuperReferences());
		}
		
	}
	
	public void printConcreteSymbols() {
		for(Symbol symbol : symbols) {
			System.out.println( symbol.getType() + " symbol : " + symbol.getFullName());
			List<VisualRepresentation> visualRepresentations = symbol.getVisualRepresentations();
			System.out.println("Visual Representation : ");
			for(VisualRepresentation visualRepresentation : visualRepresentations) {
				System.out.println(" --------------- ");
				System.out.println("\t " + visualRepresentation.getShape());
				System.out.println("\t " + visualRepresentation.getSize());
				System.out.println("\t " + visualRepresentation.getPosition());
				System.out.println("\t " + visualRepresentation.getColour());
			}
		}
	}
	
}