# AutoTech

[![Testes](https://github.com/igorparreirap/autotech-android/actions/workflows/testes.yml/badge.svg)](https://github.com/igorparreirap/autotech-android/actions/workflows/testes.yml)

Aplicativo Android para o proprietário de veículos controlar a manutenção por tempo e por quilometragem. O AutoTech registra manutenções e abastecimentos, acompanha o ciclo de cada item e avisa quando a próxima troca se aproxima, pelo critério que vencer primeiro. Os dados ficam no próprio aparelho e o aplicativo funciona sem internet.

Projeto desenvolvido como Trabalho de Conclusão de Curso em Engenharia de Computação na Pontifícia Universidade Católica de Goiás (PUC Goiás), 2026.

## Funcionalidades

- Cadastro de vários veículos, com preenchimento dos dados pela placa e cache local das consultas
- Registro de manutenções por parte do carro, com comprovante, mão de obra separada e data do serviço diferente da data do registro
- Registro de abastecimentos, com cálculo de consumo pelo método tanque a tanque
- Ciclos de troca por quilômetros, por meses ou pelos dois, com avisos escalonados e botões **Já fiz** e **Lembrar amanhã**
- Regras de consistência do odômetro, inclusive entre registros de datas diferentes
- Painel financeiro com indicadores, gráficos por mês, ano e quilometragem e divisão por categoria
- Relatório do veículo em PDF, com visualização no aplicativo e compartilhamento
- Busca de oficinas, borracharias e postos próximos
- Comparador entre álcool e gasolina pela regra dos 70%
- Busca de funções e registros a partir da tela inicial
- Acesso com e-mail e senha (hash BCrypt) ou com a conta Google, e recuperação de senha por código enviado ao e-mail

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin 2.2 (Android 8.0, API 26, ou superior) |
| Interface | Jetpack Compose, Material 3, Navigation Compose |
| Dados | Room 2.8 sobre SQLite, com migrações |
| Rede | Retrofit, OkHttp e Moshi, com limite de 5 segundos por chamada |
| Serviços | SimplesAPI (placas), Google Maps e Places, Credential Manager (Google), EmailJS |
| Segundo plano | WorkManager e notificações |
| Testes | JUnit 4 |

## Arquitetura

O código segue três camadas, com a regra da dependência da Arquitetura Limpa:

```
app/src/main/java/com/autotech/android/
├── domain/   regras de negócio em Kotlin puro (sem Android)
├── data/     Room, repositórios, mapeadores e clientes HTTP
├── work/     verificação dos alertas e notificações
├── util/     arquivos: fotos, comprovantes e relatório em PDF
└── ui/       telas, componentes e navegação
```

O pacote `domain` não importa nenhuma classe do Android, do banco ou da interface. É a camada de dados que converte os registros do banco em estruturas do domínio.

## Como executar

1. Instale o Android Studio (com JDK 21).
2. Copie `local.properties.example` para `local.properties` e preencha o caminho do SDK e as chaves dos serviços. Sem as chaves, o aplicativo compila e funciona, mas a consulta de placa, o mapa, o login com Google e o envio de e-mail ficam indisponíveis.
3. Abra a pasta do projeto no Android Studio e execute a configuração **app** em um emulador ou aparelho.

## Testes

As regras de negócio têm 147 testes de unidade em `app/src/test`, que rodam na JVM, sem emulador:

```
./gradlew testDebugUnitTest
```

O relatório fica em `app/build/reports/tests/testDebugUnitTest/index.html`. Os mesmos testes rodam no GitHub Actions a cada envio para o repositório.

## Autor

Igor Parreira de Paula

## Licença

Todos os direitos reservados. O código está disponível para consulta, mas não há licença de uso, cópia ou distribuição.
