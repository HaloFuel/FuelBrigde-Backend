@Notification @Contract @US-30
Feature: Contract with the Ordering publisher
  As the Ordering bounded context
  I want the notification service to accept the exact request my publisher sends
  So that buyers are notified when their orders change state

  # Provider side of the contract. The consumer side lives in the platform
  # (monolith) as NotificationServiceClientTest, which asserts that Ordering sends
  # {userId, type, title, message, referenceId} with a service JWT and no companyId.

  Scenario Outline: The publisher's request for a <type> event is accepted
    Given the Ordering publisher is authenticated with its service token
    When the publisher notifies user 4242 that order 7001 was "<status>" with type "<type>"
    Then the publisher's request is accepted with status 201
    And the stored notification is addressed to user 4242
    And the stored notification is an unread "<type>" about order 7001

    Examples:
      | type             | status     |
      | ORDER_CONFIRMED  | confirmado |
      | ORDER_CANCELLED  | cancelado  |
      | ORDER_DISPATCHED | despachado |

  Scenario: The publisher's notification appears among the user's unread notifications
    Given the Ordering publisher is authenticated with its service token
    And the publisher notifies user 4242 that order 7002 was "despachado" with type "ORDER_DISPATCHED"
    When user 4242 checks the unread notifications
    Then the unread notifications include order 7002
