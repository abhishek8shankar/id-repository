package io.mosip.credential.request.generator.interceptor;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.http.MediaType;

import io.mosip.credential.request.generator.constants.ApiName;
import io.mosip.credential.request.generator.constants.CredentialRequestErrorCodes;
import io.mosip.credential.request.generator.dto.CryptomanagerRequestDto;
import io.mosip.credential.request.generator.entity.CredentialEntity;
import io.mosip.credential.request.generator.exception.CredentialRequestGeneratorUncheckedException;
import io.mosip.credential.request.generator.util.RestUtil;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.util.DateUtils;

/**
 * @author Manoj SP
 *
 */
public class CredentialTransactionInterceptor extends EmptyInterceptor {

	private static final String REQUEST = "request";

	private transient RestUtil restUtil;

	private static final long serialVersionUID = 1L;

	public CredentialTransactionInterceptor(RestUtil restUtil) {
		this.restUtil = restUtil;
	}

	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		encryptData(entity, state, propertyNames);
		return super.onSave(entity, id, state, propertyNames, types);
	}

	@Override
	public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		if (entity instanceof CredentialEntity) {
			CredentialEntity credEntity = (CredentialEntity) entity;
			String encryptedData = encryptDecryptData(ApiName.DECRYPTION, credEntity.getRequest());
			credEntity.setRequest(encryptedData);
			int indexOfData = Arrays.asList(propertyNames).indexOf(REQUEST);
			state[indexOfData] = encryptedData;
		}
		return super.onLoad(entity, id, state, propertyNames, types);
	}

	@Override
	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
		encryptData(entity, currentState, propertyNames);
		return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
	}

	private void encryptData(Object entity, Object[] state, String[] propertyNames) {
		if (entity instanceof CredentialEntity) {
			CredentialEntity credEntity = (CredentialEntity) entity;
			String encryptedData = encryptDecryptData(ApiName.ENCRYPTION,
					CryptoUtil.encodeToURLSafeBase64(credEntity.getRequest().getBytes()));
			credEntity.setRequest(encryptedData);
			int indexOfData = Arrays.asList(propertyNames).indexOf(REQUEST);
			state[indexOfData] = encryptedData;
		}
	}

	private String encryptDecryptData(ApiName api, String request) {
		try {
			RequestWrapper<CryptomanagerRequestDto> requestWrapper = new RequestWrapper<>();
			CryptomanagerRequestDto cryptoRequest = new CryptomanagerRequestDto();
			cryptoRequest.setApplicationId("ID_REPO");
			cryptoRequest.setData(request);
			cryptoRequest.setReferenceId("CREDENTIAL_REQUEST");
			requestWrapper.setRequest(cryptoRequest);
			cryptoRequest.setTimeStamp(DateUtils.getUTCCurrentDateTime());
			requestWrapper.setRequest(cryptoRequest);
			ResponseWrapper<Map<String, String>> restResponse = restUtil.postApi(api, null, null, null,
					MediaType.APPLICATION_JSON_UTF8, requestWrapper, ResponseWrapper.class);
			return restResponse.getResponse().get("data");
		} catch (Exception e) {
			throw new CredentialRequestGeneratorUncheckedException(
					CredentialRequestErrorCodes.ENCRYPTION_DECRYPTION_FAILED, e);
		}
	}

	public void setRestUtil(RestUtil restUtil) {
		this.restUtil = restUtil;
	}
}