package ru.gb.lesson5;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Duration;
import java.util.*;

public class ChatClient {

  private static ObjectMapper objectMapper = new ObjectMapper();

  public static void main(String[] args) {

    Scanner console = new Scanner(System.in);

    // 127.0.0.1 или localhost
    try (Socket server = new Socket("localhost", 8888)) {
      System.out.println("Сервер ждет подключения");

      try (PrintWriter out = new PrintWriter(server.getOutputStream(), true)) {
        Scanner in = new Scanner(server.getInputStream());


        while (true) {
          System.out.print("Ваше имя: ");
          String loginRequest = createLoginRequest(console.nextLine());
          out.println(loginRequest);

          String loginResponseString = in.nextLine();

          if(!checkLoginResponse(loginResponseString))
            // TODO: Можно обогатить причиной, чтобы клиент получал эту причину
            // (логин уже занят, ошибка аутентификации\авторизации, ...)
            System.err.println("Не удалось подключиться к серверу");
          else {
            System.out.println("Вы успешно подключились к серверу");
            break;
          }
        }

        // client <----------------> server
        // client getUsers ->        server
        // client <- (getUsers|sendMessage from client) server    <--------sendMessage client2
        //

        // Отдельный поток на чтение сообщений
//        ServerListener serverListener = new ServerListener(in);
//        new Thread(serverListener).start();
        new Thread(() -> {
          while (true) {
            // TODO: парсим сообщение в AbstractRequest
            //  по полю type понимаем, что это за request, и обрабатываем его нужным образом
            try {
              String msgFromServer = in.nextLine();
              System.out.println(msgFromServer);
            } catch (Exception e) {
              break;
            }
          }
        }).start();


        while (true) {
          System.out.println("Что хочу сделать?");
          System.out.println("1. Послать сообщение другу");
          System.out.println("2. Послать сообщение всем");
          System.out.println("3. Получить список логинов");
          System.out.println("4. Выход");


          String type = console.nextLine();

          if (type.equals("1")) {
            System.out.print("Кому: ");
            String login = console.nextLine();
            System.out.print("Cообщение: ");
            String message = console.nextLine();
            SendMessageRequest request = new SendMessageRequest(login, message);
            String sendMsgRequest = objectMapper.writeValueAsString(request);
            out.println(sendMsgRequest);

          } else if (type.equals("2")) {
            System.out.print("Cообщение: ");
            String message = console.nextLine();
            BroadcastRequest request = new BroadcastRequest(message);
            String sendMsgRequest = objectMapper.writeValueAsString(request);
            out.println(sendMsgRequest);

          } else if (type.equals("3")) {
            ListClientsRequest request = new ListClientsRequest();
            String sendMsgRequest = objectMapper.writeValueAsString(request);
            out.println(sendMsgRequest);

          } else if (type.equals("4")) {
            DisconnectRequest request = new DisconnectRequest();
            String sendMsgRequest = objectMapper.writeValueAsString(request);
            out.println(sendMsgRequest);
            break;
          }

        }
      }
    } catch (IOException e) {
      System.err.println("Ошибка во время подключения к серверу: " + e.getMessage());
    }

    System.out.println("Нет подключения к серверу");
  }

  private static String createLoginRequest(String login) {
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setLogin(login);

    try {
      return objectMapper.writeValueAsString(loginRequest);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Ошибка JSON: " + e.getMessage());
    }
  }

  private static boolean checkLoginResponse(String loginResponse) {
    try {
      LoginResponse resp = objectMapper.reader().readValue(loginResponse, LoginResponse.class);
      return resp.isConnected();
    } catch (IOException e) {
      System.err.println("Ошибка чтения JSON: " + e.getMessage());
      return false;
    }
  }

  private static void sleep() {
    try {
      Thread.sleep(Duration.ofMinutes(5));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
