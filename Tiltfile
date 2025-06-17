# Tiltfile para orquestrar o ambiente de desenvolvimento completo no Kubernetes

# ------------------------------------------------------------------
# ETAPA 1: Carregar Extensões e Manifestos
# ------------------------------------------------------------------
load('ext://k8s_yaml_glob', 'k8s_yaml_glob')

# Aplica TODOS os manifestos .yaml da pasta 'k8s' no cluster Kubernetes.
# Esta única linha cuida da implantação de toda a infraestrutura (Postgres, Redis, etc.)
# e também das "carcaças" das nossas aplicações (Deployments, Services, etc.).
# all of the .yaml files in the k8s directory.
k8s_yaml_glob('./infra/dev/*.yaml')  

# ------------------------------------------------------------------
# ETAPA 2: Definir como Construir as Imagens dos Seus Serviços
# ------------------------------------------------------------------

# O Tilt usa esta seção para saber qual Dockerfile usar para cada serviço.
# O nome da imagem aqui (ex: 'event-ticket-application_booking') deve ser o mesmo
# que está no seu arquivo k8s/booking.yaml, no campo 'spec.template.spec.containers.image'.

docker_build('event-ticket-application_discovery', './servicediscovery')
docker_build('event-ticket-application_recomendation', './recomendation')
docker_build('event-ticket-application_apigateway', './api-gateway')

# Serviço 'booking' com a configuração de Live Update para desenvolvimento rápido
docker_build('event-ticket-application_booking', './event-ticket-api',
  live_update=[
    # Sincroniza os arquivos .class compilados do seu build local para dentro do contêiner,
    # evitando a necessidade de reconstruir a imagem a cada mudança no código.
    sync('./event-ticket-api/build/classes/java/main', '/app/classes'),
    
    # Sincroniza também os recursos, como o application.properties, caso ele mude.
    sync('./event-ticket-api/build/resources/main', '/app/resources')
  ]
)


# ------------------------------------------------------------------
# ETAPA 3: Configurar a Interface do Tilt e o Acesso Local
# ------------------------------------------------------------------

# k8s_resource nos permite dar nomes amigáveis na UI do Tilt e configurar
# o redirecionamento de portas (port_forwards) para que possamos acessar
# os serviços a partir da nossa máquina local via 'localhost:<porta>'.

# --- Serviços da Aplicação ---
k8s_resource(workload='discovery-deployment', new_name='discovery', port_forwards='8761')
k8s_resource(workload='apigateway-deployment', new_name='apigateway', port_forwards='8081')
k8s_resource(workload='recomendation-deployment', new_name='recomendation', port_forwards='8082')
k8s_resource(workload='booking-deployment', new_name='booking', port_forwards='8080')

# --- Serviços de Infraestrutura ---
k8s_resource(workload='postgres-deployment', new_name='postgres') # Port-forward não é essencial, pois 'booking' o acessa internamente
k8s_resource(workload='redis-deployment', new_name='redis')
k8s_resource(workload='rabbitmq-deployment', new_name='rabbitmq', port_forwards=['5672', '15672'])