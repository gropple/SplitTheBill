package com.splitthebill

import cpframework_shared.HasUUID

class Person extends HasUUID {
  var name: String = ""
  var avatarFilename: String = ""

  // Transient properties
  var checked: Boolean = false

  var owesItemsRaw: String = ""
  var owesOthersRaw: String = ""

  var owesItems: Double = 0
  var owesOthers: Double = 0

  var splittingItems = true
  var splittingOthers = true

  var paid = 0.0
  var paidRaw = ""
  var tookChange = 0.0
  var tookChangeRaw = ""

  // Capped at actual change
  var owedChange = 0.0

  // Uncapped
  var owedReal = 0.0

  def owes = owesItems + owesOthers

  def isOwed = overPayer || tookTooLittleChange
  def isOwer = underPayer || tookTooMuchChange
  
  def overPayer = paid >= (owes + 0.001)
  def underPayer = paid <= (owes - 0.001)

  def didTakeChange = tookChange > 0.001
  def tookTooMuchChange = tookChange > (owedReal + 0.001)
  def tookTooLittleChange = tookChange < (owedReal - 0.001)


  def copyTransients(p: Person) = {
    checked = p.checked
//    owesItemsClaimed = p.owesItemsClaimed
//    owesOthersClaimed = p.owesOthersClaimed
    owesItemsRaw = p.owesItemsRaw
    owesOthersRaw = p.owesOthersRaw
    owesItems = p.owesItems
    owesOthers = p.owesOthers
    splittingItems = p.splittingItems
    splittingOthers = p.splittingOthers
    paid = p.paid
    paidRaw = p.paidRaw
    tookChange = p.tookChange
    tookChangeRaw = p.tookChangeRaw
    owedChange = p.owedChange
    owedReal = p.owedReal
  }
}
