# SystemCompareTestLayout — novo layout

Cópia do projeto SystemCompare (sem as pastas de build) com o visual refeito,
inspirado no painel de referência: fundo cinza claro, cartões brancos bem
arredondados, blocos pretos, verde neon como destaque e botões em pílula.

Só a camada visual mudou. Banco (Room), repositórios, SimplesAPI, Google
Sign-In, Places e alertas são os mesmos do projeto original.

## Arquivos alterados
- ui/theme/Theme.kt — paleta (AtColors), tipografia e formas
- ui/components/AtComponents.kt — NOVO: cartões, botões, campos, blocos,
  seletor segmentado, medidor em arco, placa Mercosul, mapa decorativo
- ui/screens/*.kt — todas as telas redesenhadas (MapScreen: só a parte visual)
- ui/screens/HomeLocationCard.kt — NOVO: mapa real do Google + busca no Places
  na tela inicial (oficinas/borracharias/postos em até 5 km, rota no navegador),
  logo acima dos atalhos Manutenção / Abastecer / Veículos
- ui/navigation/AutoTechNav.kt — transições entre telas e novos atalhos da Home
- MainActivity.kt e res/values/themes.xml — barras do sistema e fundo

## Ajustes (rodada 3)
- Foto do carro: botão de câmera no cartão "Meu veículo" (e nos detalhes) para
  adicionar/trocar/remover a foto. Salva em files/vehicle_photos (sem mudar o banco).
  Arquivos: util/VehiclePhotoStore.kt, ui/components/VehiclePhoto.kt
- Sininho: abre "Notificações recentes" (ui/screens/NotificationsScreen.kt),
  com manutenções vencidas e próximas (80% ou mais do intervalo) de todos os veículos.
- Abastecimentos: calculadora álcool x gasolina pela regra dos 70%
  (ui/screens/FuelCalculatorCard.kt).

## Ajustes (rodada 4) — datas do serviço/abastecimento e do registro
- Caixa "Mesma data para o serviço/abastecimento e o registro" (marcada por padrão):
  uma data preenche as duas. Desmarcada: registro = hoje; serviço escolhido no
  calendário (sem digitação, nunca depois do registro nem no futuro).
- Banco versão 4 com migração 3->4 (campo registeredAtMillis; registros antigos
  recebem a própria data do evento). Os dados já cadastrados são mantidos.
- Arquivos: ui/components/AtDateFields.kt, MaintenanceScreens.kt, FuelScreens.kt,
  entity/MaintenanceRecordEntity.kt, entity/FuelRecordEntity.kt, AutoTechDatabase.kt.
  Cópia dos arquivos antes da mudança: SystemCompare\_layout_tmp\antes_datas\

## Ajustes (rodada 5)
- Mapa da tela inicial: NÃO consulta mais o Google Places. Mostra só a última busca
  feita na tela de mapa (guardada no aparelho, mesmo após fechar o app ou mudar de
  cidade). Nova busca só quando o usuário entra no mapa e escolhe o tipo de serviço.
  Arquivos: ui/screens/NearbyHistory.kt, ui/screens/HomeLocationCard.kt, MapScreen.kt.
- Ícone do app = logo do app (ícone adaptativo: res/mipmap-*, mipmap-anydpi-v26,
  values/ic_launcher_background.xml). O drawable antigo ic_launcher_simple não é mais usado.
- Backup desligado: android:allowBackup="false" + res/xml/data_extraction_rules.xml
  (Android 12+ também sem cópia na nuvem nem transferência entre aparelhos).
- Cópia dos arquivos antes da mudança: SystemCompare\_layout_tmp\antes_icone\

## Ajustes (rodada 6)
- Mapa da tela inicial sempre visível, centralizado na posição atual do usuário
  (Maps SDK for Android: carregamento de mapa sem custo). Mostra os estabelecimentos
  da última busca salva; sem busca, mostra "Nenhuma busca ainda" + botão Buscar.
  Sem permissão de localização: botão "Mostrar minha localização" no mapa.
  O Places continua sendo consultado só na tela de mapa.
- Cópia antes da mudança: SystemCompare\_layout_tmp\antes_mapa_home\

## Ajustes (rodada 7) — cores da logo
- Verde e preto trocados pela paleta da logo: azul #1569CC (botões, blocos, seleção),
  azul-claro #74B8F0 (destaques, medidores), azul-marinho #0E2A57 (caixas escuras),
  fundo #EEF2F8. Branco sobre o azul dos botões: contraste 5,4:1 (RNF-04).
- Cópia antes da mudança: SystemCompare\_layout_tmp\antes_azul\

## Ajustes (rodada 8) — dashboard novo e mapa da tela inicial
- Mapa da tela inicial: menor (170 dp), só com a posição atual do usuário; botões
  "Buscar serviços" e "Mostrar minha localização". Não usa mais o histórico de busca
  (NearbyHistory.kt saiu do projeto; está em _layout_tmp\antes_dashboard\).
- Dashboard (DashboardScreen.kt) refeito, inspirado no painel de referência:
  filtros por veículo e período (este mês, 6 meses, este ano, tudo, personalizado);
  indicadores com variação vs período anterior (total, manutenção, combustível,
  custo por km, consumo médio km/L, preço médio do litro); barras interativas por
  mês, ano ou faixa de km (km só com 1 veículo); rosca por categoria + tabela;
  resumo rápido; últimos lançamentos. Sempre pela data do serviço/abastecimento.
- Cópia antes da mudança: SystemCompare\_layout_tmp\antes_dashboard\

## Ajustes (rodada 9) — busca, exclusão de veículo e correção do dashboard
- Busca no app (ui/screens/SearchScreen.kt, rota "search"): a barra do topo da tela
  inicial abre a busca. Encontra funções (odômetro, meu veículo, manutenções,
  abastecimentos, calculadora, dashboard, notificações, localização...) e dados
  (veículos por marca/modelo/placa; manutenções por item/descrição/data).
  Ignora acentos e maiúsculas.
- Excluir veículo (tela de detalhes): caixa "apagar também manutenções, abastecimentos
  e lembretes". Desmarcada = veículo some das listas, histórico fica no Dashboard e
  volta se a placa for cadastrada de novo. Marcada = apaga tudo (inclui foto e
  comprovantes). Banco v5 (MIGRATION_4_5: coluna vehicles.archived).
- Dashboard: corrigido o travamento ao trocar o período (o balão animado lia a barra
  antiga pela posição; agora recebe os dados da barra).
- Cópia antes da mudança: SystemCompare\_layout_tmp\antes_busca_excluir\

## Ajustes (rodada 10) — quilometragem e comprovantes
- Nova manutenção: caixa "Mesmo KM para o serviço e o registro" (marcada por padrão;
  um campo preenche os dois). Desmarcada: KM do serviço e KM atual separados.
- Comprovantes (manutenção e abastecimento): qualquer tipo de arquivo (foto, PDF,
  XML da NF...). Guardados em files/comprovantes com nome padronizado:
  Manutencao_<Modelo-Placa>_<Item>_<AAAA-MM-DD>.ext / Abastecimento_<Modelo-Placa>_<AAAA-MM-DD>.ext
  (se repetir, ganha _2, _3...). Botão de clipe no histórico abre o arquivo.
  util/ReceiptFileStore.kt, ui/components/ReceiptAttachment.kt; FileProvider no manifesto.
- Cópia antes da mudança: SystemCompare\_layout_tmp\antes_comprovantes\

## Ajustes (rodada 11) — nome personalizado do veículo
- Detalhes do veículo: lápis ao lado do nome abre "Nome do veículo". O apelido aparece
  em todo o app (tela inicial, listas, busca, dashboard, notificações, nome dos
  comprovantes). "Restaurar nome original" volta para marca + modelo da consulta da placa.
- Banco v6 (MIGRATION_5_6: coluna vehicles.nickname). domain/VehicleNames.kt.
- Cópia antes da mudança: SystemCompare\_layout_tmp\antes_apelido\

## Atenção
Usa o mesmo applicationId (com.autotech.android): instalar esta versão
substitui a original no celular (os dados do app são mantidos). Depois de
instalar esta versão (banco v4), voltar a instalar o projeto original (banco v3)
apaga os dados de teste.

## Rodada 12 — RNF-01 (camadas) e RNF-02 (segurança) — 21/10/2026
- Arquitetura em 3 camadas: apresentação (ui), domínio (domain, Kotlin puro) e persistência (data).
- Regras tiradas das telas para o domínio: MaintenanceCycles (ciclos/alertas, RN-01/RN-04), FuelComparison (RN-06),
  SpendingAnalytics (painel), VehicleNaming (apelido). Conversão banco -> domínio em data/mapper/DomainMappers.kt.
- domain/VehicleNames.kt (usava a entidade do banco) virou ui/components/VehicleNames.kt; textos dos ciclos em CycleText.kt.
- Validação: domain compilado SOZINHO só com Kotlin+Java (imports: java.util e kotlin.math) e 33 testes passando;
  app inteiro compilado (0 erros).
- RNF-02: BCrypt com custo 12 fixo (AuthRepository); token do Google não é gravado (comentário no AuthScreen).
- Backup das fontes antigas: _layout_tmp/antes_rnf. main.tex: backup main.tex.bak_20261021_antes_rnf.

## Rodada 13 — combustível, atalhos e abastecimento no topo — 21/10/2026
- Selo "Ativo" no "Meu veículo": era o texto padrão quando o campo combustível estava vazio (a placa do carro diesel
  não trouxe combustível e o campo de texto ficou em branco). Agora mostra o combustível ou "Informar combustível".
- Combustível vira escolha obrigatória no cadastro: Flex, Gasolina, Diesel, Híbrido (domain/FuelType.kt; elétrico não
  é atendido). A consulta da placa preenche sozinha quando reconhece o texto ("ALCOOL / GASOLINA" -> Flex).
  Carros já cadastrados: tocar em "Combustível ✎" na ficha do veículo para informar/corrigir. Sem mudança de banco
  (coluna fuel já existia; grava "FLEX", "DIESEL"...).
- Atalhos: critério único — todos brancos com ícone azul; azul cheio só na ação principal (Abastecer).
- Home: seletor de veículo + barra "Abastecer" (azul) / "Manutenção" (branco) no topo, acima do cartão do veículo;
  abrem direto o formulário. No grid, "Abastecer" virou "Combustível" (lista + calculadora).
- Validação: app compilado (0 erros); 20 testes do FuelType + 33 do domínio passando.
- Backup: _layout_tmp/antes_combustivel. main.tex: main.tex.bak_20261021_antes_combustivel.

## Rodada 14 — só "Abastecer" no topo — 21/10/2026
- Removido o botão "Manutenção" da barra do topo; "Abastecer <veículo>" ocupa a largura toda e abre direto o formulário.
- Validação: só o que mudou (compilação do app, 0 erros). Backup: _layout_tmp/antes_so_abastecer.

## Rodada 15 — "Abastecer" abaixo do veículo + relatório em PDF novo — 21/10/2026
- Home: botão "Abastecer" (sem o nome do carro) logo abaixo do cartão "Meu veículo".
- PDF (util/VehicleReportPdf.kt, substitui MaintenancePdfWriter): identidade do app (fundo cinza-azulado, cartões
  brancos, faixa azul com logo e "AutoTech", cabeçalho com logo em todas as páginas, rodapé com placa/data/página).
  Ordem: veículo (foto, nome, placa, odômetro, ano, cor, combustível) -> totais -> manutenções (data, km, item,
  valor, descrição) -> abastecimentos (opcional, tabela com total) -> comprovantes (opcional; imagens e PDFs viram
  páginas, até 20 páginas por PDF; outros formatos só pelo nome).
- Detalhes do veículo: "Gerar PDF" abre a janela de opções (Abastecimentos marcado, Comprovantes desmarcado, com as
  quantidades); ao gerar, abre o visualizador. Cartão "Relatórios gerados" (3 mais recentes).
- Visualizador (PdfViewerScreen + util/PdfPages): páginas renderizadas no app, zoom com dois dedos ou toque duplo,
  botões "Compartilhar" (WhatsApp, e-mail...) e "Abrir com...". Arquivos em Android/data/.../files/Documents.
- Validação: compilação do app (0 erros). O desenho do PDF só pode ser conferido no aparelho.
- Backup: _layout_tmp/antes_pdf (inclui o MaintenancePdfWriter antigo). main.tex: main.tex.bak_20261021_antes_pdf.

## Rodada 16 — acesso ao PDF pela tela inicial — 21/10/2026
- O "Gerar PDF" só existia dentro dos detalhes do veículo (escondido). Agora:
  - Home: atalho "Relatório PDF" no lugar de "Novo veículo" (cadastro continua em Veículos > + e no cartão vazio);
    abre a janela de opções ali mesmo e, ao gerar, o visualizador.
  - Busca: "Relatório em PDF" abre os detalhes do veículo já com a janela aberta (rota vehicle/{id}?pdf=true).
  - Janela única (ui/screens/VehicleReportDialog.kt) usada na Home e nos detalhes.
- Validação: compilação do app (0 erros). Backup: _layout_tmp/antes_pdf_home.

## Rodada 17 — notificações escalonadas (RF-06 / RN-03) — 21/10/2026
- Regra (domain/ReminderSchedule.kt): tempo 30/15/7 dias antes num ciclo de 12 meses, proporcional para outros
  (6 meses: 15/8/4; mínimo 1 dia); km quando falta 50%/10%/5% do intervalo (10.000 km: 5.000/1.000/500);
  aviso de vencido. Cada etapa uma vez por ciclo (work/ReminderStore, SharedPreferences); novo serviço = ciclo novo.
  Se um critério venceu, só o aviso de vencido.
- Motor (work/MaintenanceAlertWorker): a cada 4 h em segundo plano (persiste após reiniciar), fora das 22h–8h;
  e 5 s após qualquer mudança em odômetro/manutenção/ciclo/abastecimento (observador do Room no AutoTechApplication).
- Notificação (work/ReminderNotifier): canal "Lembretes de manutenção", ícone de chave (res/drawable/ic_stat_autotech),
  toque abre o histórico de manutenções do veículo (MainActivity singleTop + ui/navigation/PendingOpen).
- Permissão de notificações pedida na tela inicial (Android 13+); tela de notificações mostra aviso + botão
  "Ativar notificações" quando desligadas, e um cartão explicando os prazos.
- Validação: 25 testes da regra; app compilado (0 erros; o ícone novo só entra no R no build do Android Studio).
- Backup: _layout_tmp/antes_notificacoes. main.tex: main.tex.bak_20261021_antes_notificacoes.

## Rodada 18 — consumo (km/L) e litros de álcool/gasolina — 21/10/2026
- Novo abastecimento: caixa "Calcular consumo (km/L)" em qualquer veículo; em flex, caixa "Especificar litros de
  álcool e de gasolina" (dois campos; o total é a soma). Prévia ao vivo: "Rodou 450 km com álcool desde o
  abastecimento de 05/10 (10.000 km): 9,0 km/L".
- Cálculo tanque a tanque (domain/FuelConsumption.kt): km desde o abastecimento anterior / litros deste.
  Combustível queimado = o do abastecimento ANTERIOR (que abriu o período).
- Lista de abastecimentos: cartão "Consumo" (média; no flex, com álcool / com gasolina e "o álcool rendeu X% da
  gasolina"); cada abastecimento mostra km/L e a composição. Calculadora álcool x gasolina só para flex (ou sem
  combustível informado).
- Banco v7 (MIGRATION_6_7): fuel_records.ethanolLiters, gasolineLiters, computeConsumption.
- Validação: 17 testes do consumo; app compilado (0 erros). Migração: conferir no Android Studio.
- Backup: _layout_tmp/antes_consumo. main.tex: main.tex.bak_20261021_antes_consumo.

## Rodada 19 — Mesmo KM no abastecimento, PDF único, "Já fiz" / "Lembrar amanhã" — 21/10/2026
- Abastecimento: caixa "Mesmo KM para o abastecimento e o registro" (igual à manutenção; saiu o botão "Usar o KM atual").
- Consumo: só informa as médias (sem a frase "o álcool rendeu X% da gasolina"); por combustível só quando especificado.
- PDF: um relatório por veículo. Assinatura (SHA-1) de veículo, registros, opções, foto e comprovantes; se igual,
  reabre o salvo ("Abrir PDF"); se mudou, gera novo e apaga o antigo ("Gerar PDF"). Opções lembradas por veículo.
  Tela do veículo: cartão "Relatório do veículo". Excluir veículo com tudo apaga o relatório.
- Notificação: botões "Já fiz" (pendência com a data da notificação) e "Lembrar amanhã" (volta em 24 h)
  — work/ReminderActionReceiver. Itens pendentes não geram avisos.
- "Já fiz" também no app: tela de Manutenções (cartão "Próximas trocas") e Notificações recentes.
- Pendentes: cartão na Home, nas Manutenções (com "Desfazer") e nas Notificações; tocar abre "Completar manutenção"
  com item, data (a do "Já fiz") e intervalo do último ciclo preenchidos; faltam km, valor, descrição e comprovante.
  Salvar a manutenção do item apaga a pendência.
- Banco v8 (MIGRATION_7_8): tabela pending_maintenance.
- Validação: app compilado (0 erros); 2 testes do "Já fiz" na regra de avisos. Backup: _layout_tmp/antes_ja_fiz.

## Rodada 20 — km/L só onde foi pedido — 21/10/2026
- Painel financeiro: saiu o cartão "Consumo médio" (o painel mostra só valores gastos). Removida a estimativa
  SpendingAnalytics.averageConsumption.
- PDF: saiu "Consumo médio" do resumo (no lugar, "Total gasto"); a tabela de abastecimentos ganhou a coluna "km/L",
  preenchida só nos abastecimentos com "Calcular consumo" marcado e com um anterior (trecho entre os dois);
  no flex com litros especificados mostra "(álcool)"/"(gasolina)".
- Validação: app compilado (0 erros); testes do painel (32) passando. Backup: _layout_tmp/antes_kml_pdf.

## Rodada 21 — itens de manutenção por parte do carro (RF-04) — 21/10/2026
- domain/MaintenanceCatalog.kt: 10 partes (Motor, Câmbio e embreagem, Freios, Suspensão e direção, Pneus e rodas,
  Elétrica e bateria, Ar-condicionado, Funilaria e pintura, Vidros e acessórios, Revisão e inspeção), ~100 itens,
  principais primeiro e demais em ordem alfabética; itens por combustível (velas de ignição só fora do diesel;
  velas aquecedoras, ARLA 32 e filtro separador de água só no diesel); combustível não informado mostra tudo.
- "Outro (especificar)" em cada parte cria item próprio (chave custom:<parte>:<nome>), com ciclo, avisos e histórico
  separados; nomes iguais reaproveitam o item; aparecem depois em "Seus itens". Sem mudança no banco.
- Tela Nova manutenção: componente MaintenanceItemPicker (busca, parte do carro, Principais, Todos os itens,
  Seus itens, Outro) e faixa com o item escolhido ("Motor › Velas de ignição"). Salvar exige item.
- Chaves antigas mantidas (oil, brake, tire, battery, timing, review, other); ícones por parte do carro.
- Painel: rosca e tabela agrupam manutenção por parte do carro; "Últimos lançamentos" mostram o item.
- Validação: 18 testes do catálogo; app compilado (0 erros). Backup: _layout_tmp/antes_catalogo.

## Rodada 22 — itens da revisão, KM atual opcional e coerência por data — 21/10/2026
- Revisão (Revisão periódica / Check-up): cartão "Itens da revisão" com lista sugerida (óleo e filtro, filtro de ar,
  de combustível, de cabine, velas ou separador de água no diesel, arrefecimento, fluido de freio, pastilhas,
  rodízio, alinhamento, balanceamento, palhetas) + "Adicionar outro item" (busca no catálogo). "Intervalo em comum"
  para os itens marcados (junção) e "intervalo próprio" por item (separação). Ao salvar: registro da revisão (com
  valor, descrição e comprovante) + um registro por item ("Feito na revisão periódica de dd/MM/aaaa"), cada um
  com o seu ciclo.
- Manutenção e abastecimento: só o "KM no dia do ..." é obrigatório. Caixa "Este é o KM atual do veículo"
  (marcada sozinha quando a data é hoje). Desmarcada: "KM atual do veículo (opcional)"; em branco, o odômetro não
  muda. KM acima do odômetro registrado só com a caixa marcada (ou KM atual informado).
- Coerência por data (domain/OdometerRules.validateTimeline): um registro não pode ter KM menor que o de uma data
  anterior nem maior que o de uma data posterior (mesmo dia não compara). Mensagem cita a data e o KM em conflito.
- Validação: 11 testes (cenário jan 120.000 / mar 50.000 / out 180.000); app compilado (0 erros).
- Backup: _layout_tmp/antes_revisao_km. main.tex: main.tex.bak_20261021_antes_revisao_km.

## Rodada 23 — mão de obra separada (RF-04) — 21/10/2026
- Nova manutenção (qualquer item, inclusive revisão): caixa "Especificar mão de obra". Desmarcada: "Valor total
  (opcional)". Marcada: "Peças / produtos" + "Mão de obra" e o total somado na tela.
- maintenance_records.laborAmount (banco v9, MIGRATION_8_9); amount continua sendo o total (painel e somas iguais).
- Histórico e PDF mostram "Peças R$ X + mão de obra R$ Y" quando separado. domain/MaintenanceCost.kt.
- Validação: 6 testes; app compilado (0 erros). Backup: _layout_tmp/antes_mao_de_obra.

## Rodada 24 — "Esqueci minha senha" por e-mail (RF-01) — 21/10/2026
- EmailJS (plano gratuito, 200 envios/mês), remetente autotech.app.email@gmail.com; chaves no local.properties
  (EMAILJS_SERVICE_ID / TEMPLATE_ID / PUBLIC_KEY / PRIVATE_KEY) -> BuildConfig (build.gradle.kts).
- Login: link "Esqueci minha senha" -> ForgotPasswordScreen (e-mail -> código de 6 números -> nova senha + confirmação).
- Regras (domain/PasswordResetRules.kt): validade 15 min, 5 tentativas, reenvio após 60 s; código guardado só como
  hash BCrypt (SharedPreferences privado); nova senha com as regras do cadastro, gravada em BCrypt custo 12.
- Conta criada com Google: aviso para usar "Continuar com o Google". Sem internet: mensagem de erro.
- data/remote/email/EmailJsApi.kt (POST https://api.emailjs.com/api/v1.0/email/send), PasswordResetRepository.
- Validação: 13 testes das regras; app compilado (0 erros). O envio real não pôde ser testado daqui (rede bloqueada).
- Backup: _layout_tmp/antes_esqueci_senha. main.tex: main.tex.bak_20261021_antes_esqueci_senha; bib: emailjs2026.
- Rodada 24b: na recuperação, a nova senha não pode ser igual à anterior ("A nova senha precisa ser diferente da
  senha anterior."); o código continua valendo para tentar outra senha. 3 testes; app compilado (0 erros).

## Rodada 25 (29/10/2026) - testes automatizados
- Projeto principal agora e esta pasta (antiga SystemCompare compactada em _arquivo/SystemCompare_antigo_20261029.zip e apagada).
- 9 classes JUnit 4 em app/src/test/java/com/autotech/android/domain (147 testes, todos passaram em 29/10/2026 15:29).
- build.gradle.kts: testImplementation("junit:junit:4.13.2"). No Android Studio: botao Run da classe ou gradlew testDebugUnitTest.

## Rodada 26 (29/10/2026) - nome do usuario apos recriar a tela
- Problema: depois de girar a tela, a saudacao passava de "Boa noite, Igor" para "Boa noite, usuario".
  O nome ficava em `remember` no AutoTechRoot e so era lido do banco no login e no splash.
- Correcao (ui/navigation/AutoTechNav.kt): `displayName` passou para `rememberSaveable`, e a rota HOME
  le o nome do banco local com `produceState` sempre que a tela e composta.
- Build no Android Studio: compilou. A instalacao no emulador falhou por falta de espaco interno
  ("Requested internal only, but not enough space"), entao o teste de rotacao ainda precisa ser feito.
- Teste de rotacao (29/10/2026, feito pelo usuario apos liberar espaco no emulador): o nome continua
  na saudacao depois de girar a tela. Correcao confirmada.
