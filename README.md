In order to have the environment configured for this project you will need:

- Docker (docker desktop for macs)
- Java version 17

// TODO mudar para goparty!

Steps to run the project in your local machine
- run the command `docker compose up` (need to have docker desktop running if you are mac user)
- then run via terminal() or Intelij

To access the database via terminal:

` docker exec -it booking-clean-arch-postgres-1 psql -U myuser -d mydatabase
`
Some useful commands:
`\l` --> List all the databases
`\c` --> connect to a database
`\dt` --> list tables available

Diagram:
![img.png](img.png)

TODO:
- [ ] adicionar cache no redis para intenção de compra
- [ ] adicionar pacote valid do spring para os dto's
- [ ] rest controller advice para tratamento de erros
- [ ] adicionar qrcode para ingresso
- [ ] adicionar testes na aplicação (unitários e integração)
- [ ] fazer deploy(flyio)
- [ ] adicionar CI(Jenkins ou circleCI)
- [ ] ter logs na aplicação
- [ ] adicionar método pagamento(Stripe por exemplo)


