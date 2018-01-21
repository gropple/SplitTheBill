package com.splitthebill

import android.util.Log

trait WhoOwesWhoUI {
  var people: Seq[Person] = null
  var ignoreEvents = false

  def setPeople(_people: Seq[Person]) = {
    people = _people
  }

  def init() = {
    ignoreEvents = true

    people.foreach(v => {
      personOwesToUI(v, Shared.prettifyFloat(v.owes, 2))

      if (!v.paidRaw.isEmpty) {
        val eval = Shared.evaluateMathsExpr(v.paidRaw, true)
        if (eval.isDefined) {
          personPaidToUI(v, Shared.prettifyFloat(eval.get, 2))
          paidFromErrorToUI(v, false)
        }
        else {
          personPaidToUI(v, v.paidRaw)
          paidFromErrorToUI(v, true)
        }
      }
      else {
        personPaidToUI(v, "")
      }

      if (!v.tookChangeRaw.isEmpty) {
        val eval = Shared.evaluateMathsExpr(v.tookChangeRaw, true)
        if (eval.isDefined) {
          personTookChangeToUI(v, Shared.prettifyFloat(eval.get, 2))
          tookFromErrorToUI(v, false)
        }
        else {
          personTookChangeToUI(v, v.tookChangeRaw)
          tookFromErrorToUI(v, true)
        }
      }
      else {
        personTookChangeToUI(v, "")
      }
    })

    ignoreEvents = false
  }

  def squareMsgGetName(v:Person): String = {
    if (v.uuid.get == "me" && v.name == "Me") return "I"
    return v.name
  }
  def squareMsgGetAmOrIs(v:Person): String = {
    if (v.uuid.get == "me" && v.name == "Me") return "am"
    return "is"
  }
  def squareMsgGetOwed(v:Person, isOwed:Boolean): String = {
    if (v.uuid.get == "me" && v.name == "Me") {
      if (isOwed) return "and am owed"
      else return "and owe"
    }
    if (isOwed) return "they are owed"
    else return "they owe"
  }
  def squareMsgUnderpaid(v:Person,underPaid:Double) = s"${squareMsgGetName(v)} underpaid by ${Shared.prettifyFloat(underPaid, 2)}"
  def squareMsgUnderpaidAndTookTooMuchChange(v:Person,underPaid:Double,tookChange:Double) = s"${squareMsgGetName(v)} underpaid by ${Shared.prettifyFloat(underPaid, 2)} and took ${Shared.prettifyFloat(v.tookChange,2)} change, ${squareMsgGetOwed(v,false)} ${Shared.prettifyFloat(underPaid+tookChange,2)}"
  def squareMsgOverpaidAndTookWayTooMuchChange(v:Person,overPaid:Double,changeSurplus:Double) = s"${squareMsgGetName(v)} overpaid by ${Shared.prettifyFloat(overPaid, 2)} but also took too much change, ${squareMsgGetOwed(v,false)} ${Shared.prettifyFloat(changeSurplus,2)}"
  def squareMsgOverpaidAndTookSomeChange(v:Person,overPaid:Double,tookChange:Double) = s"${squareMsgGetName(v)} overpaid by ${Shared.prettifyFloat(overPaid, 2)} and took ${Shared.prettifyFloat(tookChange, 2)} change, ${squareMsgGetOwed(v,true)} ${Shared.prettifyFloat(Math.abs(overPaid - tookChange),2)}"
  def squareMsgOverpaidAndTookTooLittleChange(v:Person,overPaid:Double,changeDeficit:Double) = s"${squareMsgGetName(v)} overpaid by ${Shared.prettifyFloat(overPaid, 2)} and took ${Shared.prettifyFloat(changeDeficit, 2)} too little change, ${squareMsgGetOwed(v,true)} ${Shared.prettifyFloat(overPaid + changeDeficit,2)}"
  def squareMsgOverpaid(v:Person,overPaid:Double) = s"${squareMsgGetName(v)} overpaid by ${Shared.prettifyFloat(overPaid, 2)}"
  def squareMsgIsSquare(v:Person) = s"${squareMsgGetName(v)} ${squareMsgGetAmOrIs(v)} paid up"

  def recalc() = {
    val totalOwed = people.foldLeft(0.0)((a, b) => a + b.owes)
    val totalPaid = people.foldLeft(0.0)((a, b) => a + b.paid)
    val changeDue = Math.max(0, totalPaid - totalOwed)
    val notEnough =  totalPaid < (totalOwed - 0.001)
    val changeTaken = people.foldLeft(0.0)((a, b) => a + b.tookChange)
    val tooMuchTaken = changeTaken > (changeDue + 0.001)
    val changeError = s"${Shared.prettifyFloat(changeTaken, 2)} is more than the change total ${Shared.prettifyFloat(changeDue, 2)}."
    val changeVisible = changeDue >= 0.001

    totalOwedToUI(Shared.prettifyFloat(totalOwed, 2))
    totalPaidToUI(Shared.prettifyFloat(totalPaid, 2), notEnough)
    changeDueToUI(Shared.prettifyFloat(changeDue, 2))
    changeBlockVisibleToUI(changeVisible)
    changeErrorToUI(changeError, tooMuchTaken)

    people.foreach(v => {
      v.owedReal = v.paid - v.owes
      val owedChange = Math.min(Math.max(v.paid - v.owes, 0), changeDue)
      personChangeOwedToUI(v, Shared.prettifyFloat(owedChange, 2))
      v.owedChange = owedChange - v.tookChange
    })

    val sb = new StringBuilder
    sb ++= "Split the Bill summary:\n\n"

    people.foreach(p => {
      sb ++= s"${p.name} owes ${Shared.prettifyFloat(p.owes, 2)}, has paid ${Shared.prettifyFloat(p.paid, 2)}, and taken ${Shared.prettifyFloat(p.tookChange, 2)} of the ${Shared.prettifyFloat(changeDue, 2)} change.\n\n"
    })
    sb += '\n'

    people.foreach(v => {
      var msg = ""
      var ower = false
      var owee = false

      if (v.underPayer) {
        val underPaid = v.owes - v.paid

        if (changeVisible && v.didTakeChange && v.tookTooMuchChange) {
          msg = squareMsgUnderpaidAndTookTooMuchChange(v,underPaid,v.tookChange)
          ower = true
        }
        else {
          msg = squareMsgUnderpaid(v,underPaid)
          ower = true
        }
      }
      else if (v.overPayer) {
        val overPaid = v.paid - v.owes

        if (Math.abs(overPaid - v.tookChange) < 0.001) {
          msg = squareMsgIsSquare(v)
        }
        else {
          val changeSurplus = v.owedReal - v.tookChange

          if (!changeVisible) {
            msg = squareMsgOverpaid(v, overPaid)
            owee = true
          }
          else if (!v.didTakeChange) {
            msg = squareMsgOverpaid(v, overPaid)
            owee = true
          }
          else if (v.didTakeChange && v.tookTooLittleChange) {
            msg = squareMsgOverpaidAndTookSomeChange(v, overPaid, v.tookChange)
            owee = true
          }
          else if (v.didTakeChange && !v.tookTooLittleChange) {
//            val owed = Math.abs(overPaid - changeSurplus)


            if (changeSurplus < -0.0001) {
              msg = squareMsgOverpaidAndTookWayTooMuchChange(v, overPaid, Math.abs(changeSurplus))
              ower = true
            }
            else {
              msg = squareMsgOverpaid(v, overPaid)
              owee = true
//              msg = squareMsgOverpaidAndTookSomeChange(v, overPaid, v.tookChange)
//              owee = true
            }
          }
          else {
            msg = squareMsgOverpaid(v, overPaid)
            owee = true
          }
        }
      }
      else {
        msg = squareMsgIsSquare(v)
      }

      sb ++= msg
      sb ++= "\n\n"

      personSquareToUI(v, new IsSquare(msg, ower, owee))
    })

    summaryToUI(sb.toString())

  }

  def totalOwedToUI(v: String)
  def totalPaidToUI(v: String, notEnough: Boolean)
  def changeDueToUI(v: String)
  def changeBlockVisibleToUI(v: Boolean)
  def personOwesToUI(p: Person, v: String)
  def personChangeOwedToUI(p: Person, v: String)
  def changeErrorToUI(v: String, show: Boolean)
  def personSquareToUI(v: Person, square: IsSquare)
  def personPaidToUI(p: Person, v: String)
  def personTookChangeToUI(p: Person, v: String)
  def summaryToUI(v: String)

  def paidFromUI(p: Person, v: String): Unit = {
    if (ignoreEvents) { return }
    p.paidRaw = v
    val eval = Shared.evaluateMathsExpr(v, true)
    if (eval.isDefined) {
      p.paid = eval.get
      recalc()
    }
    else {
      p.paid = 0
    }
  }

  def paidFocusFromUI(p: Person, hasFocus: Boolean): Unit = {
    if (ignoreEvents) { return }
    if (hasFocus) {
      personPaidToUI(p, p.paidRaw)
    }
    else {
      ignoreEvents = true
      if (!p.paidRaw.isEmpty) {
        val eval = Shared.evaluateMathsExpr(p.paidRaw, true)
        if (eval.isDefined) {
          personPaidToUI(p, Shared.prettifyFloat(eval.get, 2))
          paidFromErrorToUI(p, false)
        }
        else {
          paidFromErrorToUI(p, true)
        }
      }
      ignoreEvents = false
    }
  }


  def tookFromUI(p: Person, v: String): Unit = {
    if (ignoreEvents) { return }
    p.tookChangeRaw = v
    val eval = Shared.evaluateMathsExpr(v, true)
    if (eval.isDefined) {
      p.tookChange = eval.get
      recalc()
    }
    else {
      p.tookChange = 0
    }
  }

  def tookFocusFromUI(p: Person, hasFocus: Boolean): Unit = {
    if (ignoreEvents) { return }
    if (hasFocus) {
      personTookChangeToUI(p, p.tookChangeRaw)
    }
    else {
      ignoreEvents = true
      if (!p.tookChangeRaw.isEmpty) {
        val eval = Shared.evaluateMathsExpr(p.tookChangeRaw, true)
        if (eval.isDefined) {
          personTookChangeToUI(p, Shared.prettifyFloat(eval.get, 2))
          tookFromErrorToUI(p, false)
        }
        else {
          tookFromErrorToUI(p, true)
        }
      }
      ignoreEvents = false
    }
  }


  def paidFromErrorToUI(p: Person, v: Boolean)
  def tookFromErrorToUI(p: Person, v: Boolean)
}
