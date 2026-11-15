@Notification @TS-26
Feature: Notification queries
  As a buyer or provider company
  I want to look up the notifications addressed to me
  So that I can review what happened with my orders

  Background:
    Given buyer company 101 is registered as user 1101 in IAM
    And provider 202 is registered as user 2202 in IAM

  Scenario: Get a notification by its id
    Given buyer company 101 has received a "ORDER_CONFIRMED" notification about order 4101
    When I request the notification about order 4101
    Then the query responds with status 200
    And the returned notification refers to order 4101 with type "ORDER_CONFIRMED"

  Scenario: Requesting an unknown notification is rejected
    When I request the notification with id 999999
    Then the query responds with status 404

  Scenario: List the notifications of a user
    Given buyer company 101 has received a "ORDER_CONFIRMED" notification about order 4101
    And buyer company 101 has received a "ORDER_DISPATCHED" notification about order 4102
    When I list the notifications of user 1101
    Then the query responds with status 200
    And the list contains notifications about orders "4101, 4102"

  Scenario: List the notifications of a buyer company
    Given buyer company 101 has received a "ORDER_CONFIRMED" notification about order 4101
    And provider 202 has received a "NEW_REQUEST" notification about order 4201
    When I list the notifications of buyer company 101
    Then the query responds with status 200
    And the list contains notifications about orders "4101"

  Scenario: List the notifications of a provider company
    Given provider 202 has received a "NEW_REQUEST" notification about order 4201
    And buyer company 101 has received a "ORDER_CONFIRMED" notification about order 4101
    When I list the notifications of provider 202
    Then the query responds with status 200
    And the list contains notifications about orders "4201"

  Scenario: A company without a registered user gets an empty list
    When I list the notifications of buyer company 999
    Then the query responds with status 200
    And the list is empty

  Scenario: List only the unread notifications of a user
    Given buyer company 101 has received a "ORDER_CONFIRMED" notification about order 4101
    And buyer company 101 has received a "ORDER_DISPATCHED" notification about order 4102
    And the notification about order 4101 has been read
    When I list the unread notifications of user 1101
    Then the query responds with status 200
    And the list contains notifications about orders "4102"
