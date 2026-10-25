# Controles de toque

Tela deitada (paisagem). Coordenadas da interface em uma tela virtual de 1280×720 que se estica para 20:9 e 21:9 sem distorcer; tudo é medido a partir das bordas, com margem extra para o entalhe da câmera.

```
┌──────────────────────────────────────────────────────────┐
│ açaí 23   caixas 4/31   ♥ x3                      [ II ] │
│                                                          │
│                                                          │
│   (joystick nasce                              ( GIRAR ) │
│    onde o polegar                                        │
│    encostar)                         ( GIRAR )  (PULAR)  │
└──────────────────────────────────────────────────────────┘
        metade esquerda                metade direita
```

| Elemento | Posição (a partir da borda) | Tamanho | Função |
|---|---|---|---|
| Joystick flutuante | qualquer ponto da metade esquerda | base 130 px de raio, zona morta 15% | frente/trás/lados, velocidade analógica |
| Pular | 150 px da direita, 150 px de baixo | raio 95 px (área de toque 1,3×) | segurar = pulo mais alto |
| Girar | 330 px da direita, 115 px de baixo | raio 72 px (área de toque 1,3×) | vira bola por 0,5 s |
| Pausa | 70 px da direita, 70 px de cima | raio 40 px | pausa (também no botão voltar) |

## Regras
- Só 2 dedos no máximo: polegar esquerdo anda, direito pula/gira.
- Modo canhoto espelha tudo (joystick à direita, botões à esquerda).
- Ajustes: tamanho dos botões 80–140%, transparência 30–100%, vibração liga/desliga.
- Generosidade: coyote time 0,12 s, buffer de pulo 0,15 s, hitbox de dano do tatu menor que a de coleta, caixas quebram ao encostar durante o giro com folga.
- Vibração: pulo 12 ms, caixa 20 ms, dano 180 ms.
- Sair do app ou receber ligação = pausa automática.
- Posição arrastável dos botões fica para a v2.
