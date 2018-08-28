package com.barath.app.cloudfoundry.service;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.RestageApplicationRequest;
import org.cloudfoundry.operations.applications.RestartApplicationRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.cloudfoundry.operations.applications.StopApplicationRequest;
import org.cloudfoundry.operations.services.CreateUserProvidedServiceInstanceRequest;
import org.cloudfoundry.operations.services.GetServiceInstanceRequest;
import org.cloudfoundry.operations.services.ServiceInstance;
import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.UpdateUserProvidedServiceInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.barath.app.cloudfoundry.config.CloudFoundryProperties;
import com.barath.app.cloudfoundry.factory.CloudFoundryContext;
import com.barath.app.model.Organization;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Service
public class CloudFoundryServiceImpl implements CloudFoundryService {
	
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final CloudFoundryContext cloudFoundryContext;
    private final CloudFoundryProperties cloudFoundryProperties;

    public CloudFoundryServiceImpl(CloudFoundryContext cloudFoundryContext,CloudFoundryProperties cloudFoundryProperties) {
        this.cloudFoundryContext = cloudFoundryContext;
        this.cloudFoundryProperties= cloudFoundryProperties;

    }
    
  
    @Override
    public Flux<ApplicationSummary> getApps(String datacenter, String org, String space){

        CloudFoundryOperations cfOperations = this.cloudFoundryContext.getCloudFoundryOperations(datacenter,org,space);
        return cfOperations.applications().list();
    }
    
    @Override
    public Flux<ApplicationSummary> getAppsByDatacenter(String datacenter){

        Flux<ApplicationSummary> applicationSummaryFlux = Flux.empty();
        List<ApplicationSummary> apps = new ArrayList<>(3);
        if( logger.isInfoEnabled()) { logger.info("get apps by datacenter {}",datacenter); }
        List<Organization> organizations = this.cloudFoundryProperties.getOrganizations().get(datacenter);
        if(organizations.isEmpty()) {
            organizations.forEach( organization -> {            		
                List<String> spaces = organization.getSpaces();
                spaces.forEach( space ->{
                    applicationSummaryFlux.concatWith(this.getApps(datacenter,organization.getName(),space));
                });
            });
        }

        return applicationSummaryFlux;
    }
    
    @Override
    public Mono<Void> startApplication(String datacenter, String org, String space, String appName){
        CloudFoundryOperations cfOperations = this.cloudFoundryContext.getCloudFoundryOperations(datacenter,org,space);
        StartApplicationRequest request = StartApplicationRequest.builder()
                                            .name(appName)
                                            .build();
         return cfOperations.applications().start(request);

    }
    
    @Override
    public Mono<Void> stopApplication(String datacenter, String org, String space, String appName){
        CloudFoundryOperations cfOperations = this.cloudFoundryContext.getCloudFoundryOperations(datacenter,org,space);
        StopApplicationRequest request = StopApplicationRequest.builder()
                .name(appName)
                .build();
        return cfOperations.applications().stop(request);
    }
    
    @Override
    public Mono<Void> restartApplication(String datacenter, String org, String space, String appName){
        CloudFoundryOperations cfOperations = this.cloudFoundryContext.getCloudFoundryOperations(datacenter,org,space);
        RestartApplicationRequest request = RestartApplicationRequest.builder()
                .name(appName)
                .build();
        return cfOperations.applications().restart(request);
    }
    
    @Override
    public Mono<Void> restageApplication(String datacenter, String org, String space, String appName){
        CloudFoundryOperations cfOperations = this.cloudFoundryContext.getCloudFoundryOperations(datacenter,org,space);
        RestageApplicationRequest request = RestageApplicationRequest.builder()
                .name(appName)
                .build();
        return cfOperations.applications().restage(request);
    }
    
    @Override
    public Flux<ServiceInstanceSummary> getServices(String datacenter, String org, String space){

        CloudFoundryOperations cfOperations = this.cloudFoundryContext.getCloudFoundryOperations(datacenter,org,space);
        return cfOperations.services().listInstances();
    }
    
    @Override
    public Mono<ServiceInstance> getServiceInstanceByName(String datacenter, String org, String space, String name){
        CloudFoundryOperations cfOperations = this.cloudFoundryContext.getCloudFoundryOperations(datacenter,org,space);
        GetServiceInstanceRequest request = GetServiceInstanceRequest.builder()
                                                    .name(name).build();
        return cfOperations.services().getInstance(request);
    }
    
    @Override
    public Mono<Void> createUserDefinedServiceInstance(String datacenter, String org, String space, String name, Map<String,String> credentials){

        CloudFoundryOperations cfOperations = this.cloudFoundryContext.getCloudFoundryOperations(datacenter,org,space);
        CreateUserProvidedServiceInstanceRequest request = CreateUserProvidedServiceInstanceRequest.builder()
                .putAllCredentials(credentials)
                .name(name)
                .build();
        return cfOperations.services().createUserProvidedInstance(request);
    }
    
    @Override
    public Mono<Void> updateUserDefinedServiceInstanceByName(String datacenter, String org, String space, String name, Map<String,String> credentials){

        CloudFoundryOperations cfOperations = this.cloudFoundryContext.getCloudFoundryOperations(datacenter,org,space);
        Mono<ServiceInstance> serviceInstanceMono = this.getServiceInstanceByName(datacenter,org,space,name);
        ServiceInstance serviceInstance = serviceInstanceMono.block();
        UpdateUserProvidedServiceInstanceRequest request = UpdateUserProvidedServiceInstanceRequest.builder()
                .putAllCredentials(credentials)
                .userProvidedServiceInstanceName(name)
                .build();
        return cfOperations.services().updateUserProvidedInstance(request);
    }


	

}