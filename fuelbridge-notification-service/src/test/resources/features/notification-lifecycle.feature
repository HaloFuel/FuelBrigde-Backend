@Notification
Feature: Notification Lifecycle
  As a buyer company
  I want to receive and read notifications about my orders
  So that I stay informed about their status

  Scenario: Creating a notification for a buyer company resolves the recipient through IAM
    Given buyer company 101 is registered as user 1101 in IAM
    When a "ORDER_DISPATCHED" notification referencing order 4101 is created for buyer company 101
    Then the response status code should be 201
    And the notification should be unread

  Scenario: Marking a notification as read updates its status
    Given buyer company 101 is registered as user 1101 in IAM
    And a "ORDER_DISPATCHED" notification referencing order 4101 exists for buyer company 101
    When the notification is marked as read
    Then the notification should be read
