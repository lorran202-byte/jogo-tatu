# Design — jogo do tatu-bola (nome provisório)

## Ideia
Um tatu-bola corre por trilhas da Amazônia e do Maranhão, girando como bola para quebrar caixotes de feira, juntando açaí e derrubando caranguejos. Plataforma 3D de corredor, partidas de 1–3 minutos por fase.

## Loop
Correr para a frente → pular buracos e degraus → girar/pular em caixas e caranguejos → chegar ao portal do fim.

## Personagem
Tatu-bola: corpo de casco amarelo-terra com faixas mais escuras, focinho e orelhas pequenas. No giro vira uma bola listrada.

## Elementos da fase
| Símbolo | Elemento | Comportamento |
|---|---|---|
| C | caixote | quebra com giro ou pulando em cima (quica); dá 3 açaís |
| B | caixote de pulo | não quebra; pular em cima lança bem alto |
| K | caixote checkpoint | quebra e marca o ponto de volta |
| V | caixote de vida | quebra e dá +1 vida |
| F / f | açaí no chão / no ar | coleta encostando; 100 = +1 vida |
| E | caranguejo | anda de lado no trecho de chão; giro derruba, pular em cima esmaga e quica; encostar fora disso = perde vida |
| _ | buraco | cair = perde vida |
| # | chão alto | degrau de 1 m; precisa pular |
| = | ponte | chão estreito de madeira |
| G | portal | fim da fase |

## Progressão
- Vidas começam em 4. Sem vidas = game over, volta ao menu.
- Fim de fase mostra tempo, açaís e caixas quebradas (x de y). Recordes salvos.
- v1: Fase 1 "Trilha do Mangue". Depois: fase de fuga (coco gigante rolando), fase lateral, chefe.

## Números
Todos em `assets/data/balance.json`. Fases em `assets/levels/*.json`, geradas por `tools/*.py`.
