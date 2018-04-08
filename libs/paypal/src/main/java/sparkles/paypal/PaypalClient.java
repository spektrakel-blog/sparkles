package sparkles.paypal;

import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;
import sparkles.support.okhttp.BasicAuthenticator;
import sparkles.paypal.oauth2.OAuth2Api;
import sparkles.paypal.oauth2.OAuth2ApiTokenAdapter;
import sparkles.paypal.oauth2.TokenAuthentication;
import sparkles.paypal.oauth2.TokenInterceptor;
import sparkles.paypal.payments.PaymentsApi;

public interface PaypalClient {
  public static final String SANDBOX = "https://api.sandbox.paypal.com";
  public static final String LIVE = "https://api.paypal.com";

  OAuth2Api oAuth2Api();

  PaymentsApi paymentsApi();

  @Data
  static class PaypalClientImpl implements PaypalClient {
    private final OAuth2Api oAuth2Api;
    private final Retrofit retrofit;

    @Override
    public OAuth2Api oAuth2Api() {
      return oAuth2Api;
    }

    @Override
    public PaymentsApi paymentsApi() {

      return retrofit.newBuilder()
        .baseUrl(retrofit.baseUrl().newBuilder()
          .addPathSegments("v1/payments/")
          .build())
        .build()
        .create(PaymentsApi.class);
    }
  }

  @Accessors(fluent = true)
  public static class Builder {

    @Setter
    private String baseUrl;

    @Setter
    private HttpLoggingInterceptor logging;

    private String clientId;
    private String secret;

    public Builder appClient(String clientId, String secret) {
      this.clientId = clientId;
      this.secret = secret;

      return this;
    }

    public PaypalClient build() {
      // OAuth2Api via basic auth
      final OAuth2Api oAuth2Api = new Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create())
        .baseUrl(baseUrl + "v1/oauth2/")
        .client(new OkHttpClient.Builder()
          .authenticator(new BasicAuthenticator(clientId, secret))
          .addInterceptor(logging)
          .build())
        .build()
        .create(OAuth2Api.class);
      // Token-based authentication
      // final OkHttpClient.Builder okHttpClient = new OkHttpClient.Builder()
      //  .addInterceptor(new TokenInterceptor(new OAuth2ApiTokenAdapter(oAuth2Api)));

      final OkHttpClient.Builder oAuthClient = new OkHttpClient.Builder();
      if (logging != null) {
        oAuthClient.addInterceptor(logging);
      }

      final TokenAuthentication tokenAuth = new TokenAuthentication.Builder()
        .baseUrl(HttpUrl.parse(baseUrl)
          .newBuilder()
          .addPathSegments("v1/oauth2")
          .build())
        .clientId(clientId)
        .secret(secret)
        .okHttpClient(oAuthClient.build())
        .build();

      oAuthClient.addNetworkInterceptor(new TokenInterceptor(tokenAuth));

      final Retrofit retrofit = new Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create())
        .baseUrl(baseUrl)
        .client(oAuthClient.build())
        .build();

      return new PaypalClientImpl(oAuth2Api, retrofit);
    }
  }

}
