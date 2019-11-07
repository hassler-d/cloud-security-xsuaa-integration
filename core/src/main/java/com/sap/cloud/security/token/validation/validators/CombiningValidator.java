package com.sap.cloud.security.token.validation.validators;

import static com.sap.cloud.security.token.validation.ValidationResults.createInvalid;

import com.sap.cloud.security.token.validation.ValidationResult;
import com.sap.cloud.security.token.validation.ValidationResults;
import com.sap.cloud.security.token.validation.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a special validator that combines several validators into one. To
 * create an instance use the {@link #builderFor} method. By default the
 * validation stops after one invalid result has been found.
 * 
 * @param <T>
 *            the type to be validated.
 */
public class CombiningValidator<T> implements Validator<T> {

	private final List<Validator<T>> validators;
	private List<String> validationErrors = new ArrayList<>();

	private final boolean stopAfterFirstInvalidResult;

	private CombiningValidator(List<Validator<T>> validators, boolean stopAfterFirstInvalidResult) {
		this.validators = validators;
		this.stopAfterFirstInvalidResult = stopAfterFirstInvalidResult;
	}

	@Override
	public ValidationResult validate(T t) {
		for (Validator<T> validator : validators) {
			ValidationResult result = validator.validate(t);
			if(!result.isValid()) {
				validationErrors.add(result.getErrorDescription());
				if(stopAfterFirstInvalidResult == true) {
					return result;
				}
			}
		}
		if(validationErrors.size() > 0) {
			return createInvalid("{} out of {} validators reported an error. Please see detailed error descriptions.", validationErrors.size(), validators.size());
		}
		return ValidationResults.createValid();
	}

	/**
	 * Creates a {@link CombiningValidatorBuilder} object.
	 * 
	 * @param toBeValidatedType
	 *            creates a builder for {@link Validator} objects that are generic
	 *            over {@link U}.
	 * @return the builder.
	 */
	public static <U> CombiningValidatorBuilder<U> builderFor(Class<U> toBeValidatedType) {
		return new CombiningValidatorBuilder<>();
	}

	public List<String> getAllErrorDescriptions() {
		return validationErrors;
	}

	public static class CombiningValidatorBuilder<U> {
		private final List<Validator<U>> validators = new ArrayList<>();
		private boolean stopAfterFirstInvalidResult = true;

		/**
		 * Add the validator to the validation chain.
		 * 
		 * @param validator
		 *            the validator used for validation.
		 * @return this builder.
		 */
		public CombiningValidatorBuilder<U> with(Validator<U> validator) {
			validators.add(validator);
			return this;
		}

		/**
		 * Causes the created validator to not stop validating after the first invalid
		 * result.
		 * 
		 * @return this builder.
		 */
		public CombiningValidatorBuilder<U> validateAll() {
			stopAfterFirstInvalidResult = false;
			return this;
		}

		/**
		 * @return the validator.
		 */
		public CombiningValidator<U> build() {
			return new CombiningValidator<>(validators, stopAfterFirstInvalidResult);
		}
	}

}
