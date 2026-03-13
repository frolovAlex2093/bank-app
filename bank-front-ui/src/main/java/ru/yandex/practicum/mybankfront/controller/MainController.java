package ru.yandex.practicum.mybankfront.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.accountsservice.entity.Account;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountResponseDto;
import ru.yandex.practicum.mybankfront.controller.dto.CashAction;
import ru.yandex.practicum.mybankfront.controller.dto.UpdateAccountRequest;
import ru.yandex.practicum.mybankfront.controller.stub.AccountStub;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Контроллер main.html.
 *
 * Используемая модель для main.html:
 *      model.addAttribute("name", name);
 *      model.addAttribute("birthdate", birthdate.format(DateTimeFormatter.ISO_DATE));
 *      model.addAttribute("sum", sum);
 *      model.addAttribute("accounts", accounts);
 *      model.addAttribute("errors", errors);
 *      model.addAttribute("info", info);
 *
 * Поля модели:
 *      name - Фамилия Имя текущего пользователя, String (обязательное)
 *      birthdate - дата рождения текущего пользователя, String в формате 'YYYY-MM-DD' (обязательное)
 *      sum - сумма на счету текущего пользователя, Integer (обязательное)
 *      accounts - список аккаунтов, которым можно перевести деньги, List<AccountDto> (обязательное)
 *      errors - список ошибок после выполнения действий, List<String> (не обязательное)
 *      info - строка успешности после выполнения действия, String (не обязательное)
 *
 * С примерами использования можно ознакомиться в тестовом классе заглушке AccountStub
 */
@Controller
@RequiredArgsConstructor
public class MainController {

    private final RestClient restClient;

    /**
     * GET /.
     * Редирект на GET /account
     */
    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    /**
     * GET /account.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для получения данных аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     */
    @GetMapping("/account")
    public String getAccount(
            Model model,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient
    ) {
        String token = authorizedClient.getAccessToken().getTokenValue();

        AccountResponseDto account = restClient.get()
                .uri("/api/accounts/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(AccountResponseDto.class);

        List<AccountDto> otherAccounts = restClient.get()
                .uri("/api/accounts/list")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        fillModel(model, account, otherAccounts, null, null);
        return "main";
    }

    /**
     * POST /account.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для изменения данных текущего пользователя по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     *
     * Изменяемые данные:
     * 1. name - Фамилия Имя
     * 2. birthdate - дата рождения в формате YYYY-DD-MM
     */
    @PostMapping("/account")
    public String editAccount(
            Model model,
            @RequestParam("name") String name,
            @RequestParam("birthdate") LocalDate birthdate,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient
    ) {
        String token = authorizedClient.getAccessToken().getTokenValue();

        // Валидация возраста
        if (birthdate.isAfter(LocalDate.now().minusYears(18))) {
            // Получаем текущие данные для перерисовки страницы
            AccountResponseDto account = restClient.get()
                    .uri("/api/accounts/me")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(AccountResponseDto.class);
            List<AccountDto> otherAccounts = restClient.get()
                    .uri("/api/accounts/list")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            fillModel(model, account, otherAccounts, List.of("Возраст должен быть старше 18 лет"), null);
            return "main";
        }

        // Разбираем "Фамилия Имя"
        String[] parts = name.trim().split("\\s+", 2);
        String firstName = parts.length > 0 ? parts[0] : "";
        String lastName = parts.length > 1 ? parts[1] : "";

        AccountResponseDto updated = restClient.put()
                .uri("/api/accounts/me")
                .header("Authorization", "Bearer " + token)
                .body(new UpdateAccountRequest(firstName, lastName, birthdate))
                .retrieve()
                .body(AccountResponseDto.class);

        List<AccountDto> otherAccounts = restClient.get()
                .uri("/api/accounts/list")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        fillModel(model, updated, otherAccounts, null, "Данные успешно сохранены");
        return "main";
    }


    /**
     * POST /cash.
     * Что нужно сделать:
     * 1. Сходить в сервис cash через Gateway API для снятия/пополнения счета текущего аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     *
     * Параметры:
     * 1. value - сумма списания
     * 2. action - GET (снять), PUT (пополнить)
     */
    @PostMapping("/cash")
    public String editCash(
            Model model,
            @RequestParam int value,
            @RequestParam CashAction action,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient
    ) {
        String token = authorizedClient.getAccessToken().getTokenValue();

        try {
            restClient.post()
                    .uri("/api/cash?value={v}&action={a}", value, action)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        throw new RuntimeException("Недостаточно средств на счету");
                    })
                    .toBodilessEntity();

            // Перечитываем актуальные данные
            return getAccountWithInfo(model, authorizedClient, null, "Операция выполнена успешно");
        } catch (Exception e) {
            return getAccountWithInfo(model, authorizedClient, List.of(e.getMessage()), null);
        }
    }

    /**
     * POST /transfer.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для перевода со счета текущего аккаунта на счет другого аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     *
     * Параметры:
     * 1. value - сумма списания
     * 2. login - логин пользователя получателя
     */

    @PostMapping("/transfer")
    public String transfer(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("login") String login,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient
    ) {
        String token = authorizedClient.getAccessToken().getTokenValue();

        try {
            restClient.post()
                    .uri("/api/transfer?toLogin={login}&amount={amount}", login, value)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        throw new RuntimeException("Недостаточно средств для перевода");
                    })
                    .toBodilessEntity();

            return getAccountWithInfo(model, authorizedClient, null, "Перевод выполнен успешно");
        } catch (Exception e) {
            return getAccountWithInfo(model, authorizedClient, List.of(e.getMessage()), null);
        }
    }

    private String getAccountWithInfo(Model model, OAuth2AuthorizedClient authorizedClient,
                                      List<String> errors, String info) {
        String token = authorizedClient.getAccessToken().getTokenValue();

        AccountResponseDto account = restClient.get()
                .uri("/api/accounts/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(AccountResponseDto.class);

        List<AccountDto> otherAccounts = restClient.get()
                .uri("/api/accounts/list")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        fillModel(model, account, otherAccounts, errors, info);
        return "main";
    }

    private void fillModel(Model model, AccountResponseDto account,
                           List<AccountDto> accounts, List<String> errors, String info) {
        model.addAttribute("name", account.firstName() + " " + account.lastName());
        model.addAttribute("birthdate", account.birthDate().format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("sum", account.balance());
        model.addAttribute("accounts", accounts != null ? accounts : List.of());
        if (errors != null) model.addAttribute("errors", errors);
        if (info != null) model.addAttribute("info", info);
    }
}
