Feature: xd delta based on a sequence of boring messages

  Scenario: Create/create/ship
    When boring message is
      | total | stock |
      | 7     | 2     |
    Then the delta is 5
    When boring message is
      | total | stock |
      | 10    | 6     |
    Then the delta is 3
    When boring message is
      | total | stock | shipped |
      | 10    | 2     | 5       |
    Then the delta is 0
    When boring message is
      | total | stock | shipped |
      | 10    | 0     | 10      |
    Then the delta is 0

  Scenario: tabled Create/create/ship
    Then ∆'s are:
      | total | stock | shipped | delta | description                                    |
      | 7     | 2     |         | 5     | new customer order                             |
      | 10    | 6     |         | 3     | customer order +3 stock +4 via our fulfillment |
      | 10    | 2     | 5       | 0     | shipment                                       |
      | 10    | 0     | 10      | 0     | final shipment                                 |

  Scenario: Create/cancel/ship
    Then ∆'s are:
      | total | stock | shipped | cancelled | delta | description                      |
      | 7     | 2     |         |           | 5     | new customer order               |
      | 7     | 2     |         | 3         | 0     | cancellations are so far ignored |
      | 7     | 0     | 4       | 3         | 0     | the rest is shipped              |

  Scenario: Create/found stock/create/ship
    Then ∆'s are:
      | total | stock | shipped | delta | description                                                                            |
      | 7     | 2     |         | 5     | new customer order                                                                     |
      | 10    | 6     |         | 3     | new customer order, order as not sure where the stock coming from xD or somewhere else |
      | 10    | 0     | 10      | 0     | all shipped                                                                            |

  Scenario: Create shipment / timing issue
    Then ∆'s are:
      | total | stock | shipped | delta | description                    |
      | 7     | 2     |         | 5     | new customer order             |
      | 7     | 2     | 2       | 0     | shipment bypassing stock       |
      | 10    | 0     | 4       | 3     | customer order +3 shipment + 2 |
      | 10    | 0     | 10      | 0     | final shipment |

  Scenario: Received goods not registered yet
    Then ∆'s are:
      | total | stock | shipped | delta | description                                          |
      | 7     | 2     |         | 5     | New customer order 7                                 |
      | 10    | 6     |         | 3     | New customer order with the stock +4 unrelated to xd |
      | 10    | 1     | 5       | 0     | shipment + noticed we've delivered that thing        |
      | 10    | 0     | 10      | 0     | final shipment                                       |

  Scenario: Stock decreased
    Then ∆'s are:
      | total | stock | shipped | delta | description                     |
      | 7     | 2     |         | 5     | New customer order              |
      | 10    | 0     |         | 5     | New customer order + 3 stock -2 |
      | 10    | 0     | 10      | 0     | final shipment                  |
