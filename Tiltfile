load('ext://k8s_yaml_glob', 'k8s_yaml_glob')

k8s_yaml_glob('./infra/dev/*.yaml')  

docker_build('event-ticket-application_discovery', './servicediscovery')
docker_build('event-ticket-application_recomendation', './recomendation')
docker_build('event-ticket-application_apigateway', './api-gateway')
docker_build('event-ticket-application_booking', './event-ticket-api')

k8s_resource(workload='discovery-deployment', new_name='discovery', port_forwards='8761')
k8s_resource(workload='apigateway-deployment', new_name='apigateway', port_forwards='8081')
k8s_resource(workload='recomendation-deployment', new_name='recomendation', port_forwards='8082')
k8s_resource(workload='booking-deployment', new_name='booking', port_forwards='8080')

k8s_resource(workload='postgres-deployment', new_name='postgres') # Port-forward não é essencial, pois 'booking' o acessa internamente
k8s_resource(workload='redis-deployment', new_name='redis')
k8s_resource(workload='rabbitmq-deployment', new_name='rabbitmq', port_forwards=['5672', '15672'])
k8s_resource(workload='prometheus-deployment', new_name='prometheus', port_forwards='9090')
k8s_resource(workload='grafana-deployment', new_name='grafana', port_forwards='3000')
