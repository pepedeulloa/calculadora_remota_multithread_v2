# Calculadora Remota Multithread con Multiplexación de Sockets (TCP/UDP)

Este repositorio contiene el código fuente para una calculadora remota implementada en Java. La aplicación consta de un servidor multithread que utiliza multiplexación de sockets para la gestión eficiente de las conexiones y un cliente para interactuar con el servidor.

## Funcionalidades

- Realiza operaciones aritméticas simples (suma, resta, multiplicación, división, módulo y factorial).
- Soporte para comunicación tanto a través de TCP como de UDP.
- Gestión multithread para manejar múltiples conexiones simultáneamente.
- Uso de multiplexación de sockets para una eficiente administración de las conexiones.

## Estructura del Repositorio

- **`tcpmpser.java`**: Código fuente del servidor que implementa la calculadora remota.
- **`tcpmpcli.java`**: Código fuente del cliente para interactuar con el servidor.

## Instrucciones de Uso

### Servidor

Para ejecutar el servidor, utiliza el siguiente comando en la terminal:

```bash
java tcpmpser <puerto>
```

### Cliente (TCP)
Para ejecutar el cliente en modo TCP, utiliza el siguiente comando en la terminal:

```bash
java tcpmpcli <ip_servidor> <puerto>
```
### Cliente (UDP)
Para ejecutar el cliente en modo UDP, utiliza el siguiente comando en la terminal:

```bash
java tcpmpcli -u <ip_servidor> <puerto>
```

### Comandos del Cliente
El cliente acepta operaciones aritméticas en el siguiente formato:

```bash
<operando1> <operador> <operando2>
```
Por ejemplo:

```bash
5 + 3
10 / 2
-7 * 4
5 !
```

Envía 'QUIT' para salir del cliente.

## Notas
 - La calculadora remota utiliza conexiones no bloqueantes y multiplexación de sockets para una gestión eficiente de múltiples conexiones.
 - Soporta tanto operaciones TCP como UDP para adaptarse a diferentes escenarios de red.
 - El código está estructurado para facilitar la extensión de funcionalidades y la incorporación de nuevas operaciones.
