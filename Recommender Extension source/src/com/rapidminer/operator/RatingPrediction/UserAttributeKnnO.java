package com.rapidminer.operator.RatingPrediction;

import java.util.List;

import com.rapidminer.data.EntityMapping;
import com.rapidminer.data.IEntityMapping;
import com.rapidminer.data.IRatings;
import com.rapidminer.data.Ratings;
import com.rapidminer.data.SparseBooleanMatrix;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;


/**
 * UserAttributeKnn operator for Rating Prediction
 * 
 * @see com.rapidminer.operator.RatingPrediction.UserAttributeKnnO
 * @see com.rapidminer.operator.RatingPrediction.UserAttributeKnn
 * 
 * @author Matej Mihelcic (Ru�er Bo�kovi� Institute)
 */


public class UserAttributeKnnO extends Operator {


			public static final String PARAMETER_K = "k";
			public static final String PARAMETER_Min="Min Rating";
			public static final String PARAMETER_Range="Range";
			public static final String PARAMETER_REGU="reg_u";
			public static final String PARAMETER_REGI="reg_i";
			
			private InputPort exampleSetInput = getInputPorts().createPort("example set");
			private InputPort exampleSetInput1=getInputPorts().createPort("user attributes");
			private OutputPort exampleSetOutput1 = getOutputPorts().createPort("Model");
			private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");

			
			
			public List<ParameterType> getParameterTypes() {
				 List<ParameterType> types = super.getParameterTypes();
				 types.add(new ParameterTypeInt(PARAMETER_K, "The used number of nearest neighbors. Range: integer; 1-+?; default: 80", 1, Integer.MAX_VALUE, 80, false));
				 types.add(new ParameterTypeInt(PARAMETER_Min, "Value of minimal rating value. Range: integer; 0-+?; default: 1", 0, Integer.MAX_VALUE, 1, false));
				 types.add(new ParameterTypeInt(PARAMETER_Range, "Range of possible rating values.  Range: integer; 1-+?; default: 4 ; Max Rating=Min Rating+Range;", 1, Integer.MAX_VALUE, 4, false));
				 types.add(new ParameterTypeDouble(PARAMETER_REGU, "Regularization parameter for user biases.  Range: double; 0-+?; default: 10 ;", 0, Double.MAX_VALUE, 10, true));
				 types.add(new ParameterTypeDouble(PARAMETER_REGI, "Regularization parameter for item biases.  Range: double; 0-+?; default: 5 ;", 0, Double.MAX_VALUE, 5, true));
				 return types;
				 }
			
			/**
			 * Constructor
			 */
			public UserAttributeKnnO(OperatorDescription description) {
				super(description);

				exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, "user identification", Ontology.ATTRIBUTE_VALUE));
				exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, "item identification", Ontology.ATTRIBUTE_VALUE));
				exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, "label", Ontology.ATTRIBUTE_VALUE));
				
				getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {
				});
				
				getTransformer().addRule(new GenerateNewMDRule(exampleSetOutput1, new MetaData(RatingPredictor.class)) {
			           
				 });
				
			}

			@Override
			public void doWork() throws OperatorException {
				
				ExampleSet exampleSet = exampleSetInput.getData();
						
						IEntityMapping user_mapping=new EntityMapping();
						IEntityMapping item_mapping=new EntityMapping();
						IEntityMapping attr_mapping=new EntityMapping();
						IRatings training_data=new Ratings();
						
						 if (exampleSet.getAttributes().getSpecial("user identification") == null) {
					            throw new UserError(this,105);
					        }
							
						 if (exampleSet.getAttributes().getSpecial("item identification") == null) {
					            throw new UserError(this, 105);
					        }
						 
						 if (exampleSet.getAttributes().getLabel() == null) {
					            throw new UserError(this, 105);
					        }
						 
						 Attributes Att = exampleSet.getAttributes();
						 AttributeRole ur=Att.getRole("user identification");
						 Attribute u=ur.getAttribute();
						 AttributeRole ir=Att.getRole("item identification");
						 Attribute i=ir.getAttribute();
						 Attribute ui=Att.getLabel();

						for (Example example : exampleSet) {
							
							double j=example.getValue(u);
							int uid=user_mapping.ToInternalID((int) j);
							
							j=example.getValue(i);
							int iid=item_mapping.ToInternalID((int) j);
							
							double r=example.getValue(ui);
							training_data.Add(uid, iid, r);
							
						}
						
						
						ExampleSet attribute_set=exampleSetInput1.getData();
						Attributes aatr = attribute_set.getAttributes();
						AttributeRole ar=aatr.getRole("attribute identification");
						Attribute at=ar.getAttribute();

						ur=aatr.getRole("user identification");
						i=ur.getAttribute();

						SparseBooleanMatrix mat=new SparseBooleanMatrix();
					
						
							for (Example example : attribute_set) {
							
							double j;

							j=example.getValue(i);
							int iid=user_mapping.ToInternalID((int) j);
							
							j=example.getValue(at);
							int aid=(int)j;
							
							mat.setLocation(iid, aid, true);
						}
						 
						 UserAttributeKnn recommendAlg=new UserAttributeKnn();
						
						 
						 int K=getParameterAsInt("k");
						 double regU=getParameterAsDouble("reg_u");
						 recommendAlg.RegU=regU;
						 double regI=getParameterAsDouble("reg_i");
						 recommendAlg.RegI=regI;
						 recommendAlg.user_mapping=user_mapping;
						 recommendAlg.item_mapping=item_mapping;
						 recommendAlg.attribute_mapping=attr_mapping;
						 recommendAlg.SetK(K);
						 recommendAlg.SetMinRating(getParameterAsInt("Min Rating"));
						 recommendAlg.SetMaxRating(recommendAlg.GetMinRating()+getParameterAsInt("Range"));
						 
						 recommendAlg.SetRatings(training_data);
						 recommendAlg.SetUserAttributes(mat);

						 recommendAlg.Train();
						 
						exampleSetOutput.deliver(exampleSet);
						exampleSetOutput1.deliver(recommendAlg);
						}
			}
