package com.spotify.apollo.environment;

import com.spotify.apollo.AppInit;
import com.spotify.apollo.Environment;
import com.spotify.apollo.request.RequestHandler;

public interface ApolloEnvironment {

  Environment environment();

  RequestHandler initialize(AppInit appInit);
}
